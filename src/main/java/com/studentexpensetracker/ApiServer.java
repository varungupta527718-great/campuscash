package com.studentexpensetracker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Small HTTP bridge between the React page and the Java tracker. */
public final class ApiServer {
    private ApiServer() { }

    public static void main(String[] args) throws Exception {
        String jdbc = System.getenv("EXPENSE_JDBC_URL");
        ExpenseRepository repository;
        if (jdbc != null && !jdbc.trim().isEmpty()) {
            repository = new JdbcExpenseRepository(jdbc, System.getenv("EXPENSE_JDBC_USER"), System.getenv("EXPENSE_JDBC_PASSWORD"));
            System.out.println("Expense storage: JDBC");
        } else {
            repository = new CsvExpenseRepository("data/expenses.csv");
            System.out.println("Expense storage: CSV");
        }

        ExpenseService service = new ExpenseService(repository);
        TrackerSettingsStore settings = new TrackerSettingsStore("data/tracker-settings.bin");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.createContext("/api/expenses", request -> handleExpenses(request, service));
        server.createContext("/api/summary", request -> handleSummary(request, service));
        server.createContext("/api/profile", request -> handleProfile(request, settings));
        server.createContext("/api/budgets", request -> handleBudgets(request, settings));
        server.createContext("/api/goals", request -> handleGoals(request, settings));
        // The HTTP server chooses its own worker threads when no executor is supplied.
        server.setExecutor(null);
        server.start();
        System.out.println("Expense API listening at http://127.0.0.1:8080");
    }

    private static void handleExpenses(HttpExchange request, ExpenseService service) throws IOException {
        addBrowserHeaders(request);
        if ("OPTIONS".equals(request.getRequestMethod())) { send(request, 204, ""); return; }
        try {
            String method = request.getRequestMethod();
            String path = request.getRequestURI().getPath();
            if ("GET".equals(method) && "/api/expenses".equals(path)) { send(request, 200, expensesJson(service.all())); return; }
            if ("POST".equals(method) && "/api/expenses".equals(path)) {
                Map<String, String> fields = SimpleJson.readObject(readBody(request));
                Expense added = service.add(required(fields, "title"), number(fields, "amount"), required(fields, "category"), required(fields, "date"), fields.get("type"));
                send(request, 201, expenseJson(added));
                return;
            }
            long id = idFromPath(path, "/api/expenses/");
            if (id > 0 && "DELETE".equals(method)) { service.remove(id); send(request, 200, "{\"ok\":true}"); return; }
            if (id > 0 && "PUT".equals(method)) {
                Map<String, String> fields = SimpleJson.readObject(readBody(request));
                service.edit(id, required(fields, "title"), number(fields, "amount"), required(fields, "category"), required(fields, "date"), fields.get("type"));
                send(request, 200, expenseJson(service.find(id)));
                return;
            }
            send(request, 404, error("Not found"));
        } catch (ExpenseNotFoundException e) { send(request, 404, error(e.getMessage())); }
        catch (Exception e) { send(request, 400, error(message(e))); }
    }

    private static void handleSummary(HttpExchange request, ExpenseService service) throws IOException {
        addBrowserHeaders(request);
        if (!"GET".equals(request.getRequestMethod())) { send(request, 405, error("Method not allowed")); return; }
        double spending = 0;
        for (Expense expense : service.all()) if (!"Income".equals(expense.getType())) spending += expense.getAmount();
        Map<String, Double> categories = new TreeMap<>();
        for (Expense expense : service.all()) {
            if (!"Income".equals(expense.getType())) addToTotal(categories, expense.getCategory(), expense.getAmount());
        }
        StringBuilder json = new StringBuilder("{\"total\":").append(money(spending))
                .append(",\"income\":").append(money(service.totalIncome())).append(",\"categories\":[");
        boolean first = true;
        for (Map.Entry<String, Double> row : categories.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append("{\"name\":").append(quote(row.getKey())).append(",\"amount\":").append(money(row.getValue())).append('}');
        }
        json.append("]}");
        send(request, 200, json.toString());
    }

    private static void handleProfile(HttpExchange request, TrackerSettingsStore store) throws IOException {
        addBrowserHeaders(request);
        if ("OPTIONS".equals(request.getRequestMethod())) { send(request, 204, ""); return; }
        if ("GET".equals(request.getRequestMethod())) { send(request, 200, stringMapJson(store.profile())); return; }
        if ("PUT".equals(request.getRequestMethod()) || "POST".equals(request.getRequestMethod())) {
            try { store.updateProfile(SimpleJson.readObject(readBody(request))); send(request, 200, stringMapJson(store.profile())); }
            catch (Exception e) { send(request, 400, error(message(e))); }
            return;
        }
        send(request, 405, error("Method not allowed"));
    }

    private static void handleBudgets(HttpExchange request, TrackerSettingsStore store) throws IOException {
        addBrowserHeaders(request);
        if ("OPTIONS".equals(request.getRequestMethod())) { send(request, 204, ""); return; }
        if ("GET".equals(request.getRequestMethod())) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Double> row : store.budgets().entrySet()) {
                if (!first) json.append(',');
                first = false;
                json.append(quote(row.getKey())).append(':').append(money(row.getValue()));
            }
            send(request, 200, json.append('}').toString());
            return;
        }
        if ("PUT".equals(request.getRequestMethod()) || "POST".equals(request.getRequestMethod())) {
            try {
                Map<String, String> fields = SimpleJson.readObject(readBody(request));
                store.setBudget(required(fields, "category"), number(fields, "amount"));
                send(request, 200, "{\"ok\":true}");
            } catch (Exception e) { send(request, 400, error(message(e))); }
            return;
        }
        send(request, 405, error("Method not allowed"));
    }

    private static void handleGoals(HttpExchange request, TrackerSettingsStore store) throws IOException {
        addBrowserHeaders(request);
        if ("OPTIONS".equals(request.getRequestMethod())) { send(request, 204, ""); return; }
        String path = request.getRequestURI().getPath();
        try {
            if ("GET".equals(request.getRequestMethod()) && "/api/goals".equals(path)) { send(request, 200, goalsJson(store.goals())); return; }
            if ("POST".equals(request.getRequestMethod()) && "/api/goals".equals(path)) {
                Map<String, String> fields = SimpleJson.readObject(readBody(request));
                SavingsGoal goal = store.addGoal(required(fields, "name"), number(fields, "target"), optionalNumber(fields, "saved"), fields.get("deadline"), fields.get("emoji"));
                send(request, 201, goalJson(goal));
                return;
            }
            boolean contribution = path.endsWith("/contributions");
            String idPath = contribution ? path.substring(0, path.length() - "/contributions".length()) : path;
            long id = idFromPath(idPath, "/api/goals/");
            if (id > 0 && contribution && "POST".equals(request.getRequestMethod())) {
                Map<String, String> fields = SimpleJson.readObject(readBody(request));
                send(request, 200, goalJson(store.contribute(id, number(fields, "amount"))));
                return;
            }
            if (id > 0 && !contribution && "DELETE".equals(request.getRequestMethod())) { store.removeGoal(id); send(request, 200, "{\"ok\":true}"); return; }
            if (id > 0 && !contribution && "PUT".equals(request.getRequestMethod())) {
                Map<String, String> fields = SimpleJson.readObject(readBody(request));
                SavingsGoal goal = store.updateGoal(id, required(fields, "name"), number(fields, "target"), optionalNumber(fields, "saved"), fields.get("deadline"), fields.get("emoji"));
                send(request, 200, goalJson(goal));
                return;
            }
            send(request, 404, error("Goal not found."));
        } catch (java.util.NoSuchElementException e) { send(request, 404, error(e.getMessage())); }
        catch (Exception e) { send(request, 400, error(message(e))); }
    }

    private static long idFromPath(String path, String prefix) {
        if (!path.startsWith(prefix)) return -1;
        String value = path.substring(prefix.length());
        if (value.isEmpty()) return -1;
        for (int i = 0; i < value.length(); i++) if (!Character.isDigit(value.charAt(i))) return -1;
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { return -1; }
    }

    private static String required(Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing field: " + key);
        return value.trim();
    }
    private static double number(Map<String, String> fields, String key) { return Double.parseDouble(required(fields, key)); }
    private static double optionalNumber(Map<String, String> fields, String key) { String value = fields.get(key); return value == null || value.trim().isEmpty() ? 0 : Double.parseDouble(value); }
    private static String message(Exception e) { return e.getMessage() == null ? "Invalid request" : e.getMessage(); }
    private static void addToTotal(Map<String, Double> totals, String key, double value) { Double current = totals.get(key); totals.put(key, (current == null ? 0 : current) + value); }
    private static String money(double value) { return MoneyTools.format(value); }

    private static String stringMapJson(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> row : values.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append(quote(row.getKey())).append(':').append(quote(row.getValue()));
        }
        return json.append('}').toString();
    }
    private static String goalsJson(List<SavingsGoal> goals) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < goals.size(); i++) { if (i > 0) json.append(','); json.append(goalJson(goals.get(i))); }
        return json.append(']').toString();
    }
    private static String goalJson(SavingsGoal goal) {
        return "{\"id\":" + goal.getId() + ",\"name\":" + quote(goal.getName()) + ",\"target\":" + money(goal.getTarget())
                + ",\"saved\":" + money(goal.getSaved()) + ",\"deadline\":" + quote(goal.getDeadline()) + ",\"emoji\":" + quote(goal.getEmoji())
                + ",\"completed\":" + goal.isCompleted() + "}";
    }
    private static String expensesJson(List<Expense> expenses) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < expenses.size(); i++) { if (i > 0) json.append(','); json.append(expenseJson(expenses.get(i))); }
        return json.append(']').toString();
    }
    private static String expenseJson(Expense expense) {
        return "{\"id\":" + expense.getId() + ",\"title\":" + quote(expense.getTitle()) + ",\"amount\":" + money(expense.getAmount())
                + ",\"category\":" + quote(expense.getCategory()) + ",\"date\":" + quote(expense.getDate()) + ",\"type\":" + quote(expense.getType()) + "}";
    }
    private static String error(String text) { return "{\"error\":" + quote(text) + "}"; }
    private static String quote(String text) {
        StringBuilder quoted = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\\') quoted.append('\\').append(ch);
            else if (ch == '\n') quoted.append("\\n");
            else if (ch == '\r') quoted.append("\\r");
            else if (ch == '\t') quoted.append("\\t");
            else if (ch == '\b') quoted.append("\\b");
            else if (ch == '\f') quoted.append("\\f");
            else if (ch < 32) appendUnicodeEscape(quoted, ch);
            else quoted.append(ch);
        }
        return quoted.append('"').toString();
    }
    private static void appendUnicodeEscape(StringBuilder text, char value) {
        String hex = Integer.toHexString(value);
        text.append("\\u");
        for (int i = hex.length(); i < 4; i++) text.append('0');
        text.append(hex);
    }
    private static String readBody(HttpExchange request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getRequestBody(), "UTF-8"))) {
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) body.append(buffer, 0, count);
        }
        return body.toString();
    }
    private static void addBrowserHeaders(HttpExchange request) {
        String origin = request.getRequestHeaders().getFirst("Origin");
        if ("http://localhost:5173".equals(origin) || "http://127.0.0.1:5173".equals(origin)) request.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
        request.getResponseHeaders().set("Vary", "Origin");
        request.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        request.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        request.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    }
    private static void send(HttpExchange request, int status, String text) throws IOException {
        byte[] result = text.getBytes("UTF-8");
        request.sendResponseHeaders(status, result.length);
        try (OutputStream output = request.getResponseBody()) { output.write(result); }
    }
}
