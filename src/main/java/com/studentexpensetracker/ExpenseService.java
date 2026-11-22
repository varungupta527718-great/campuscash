package com.studentexpensetracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Application methods for adding, editing, searching, and reporting records. */
public final class ExpenseService {
    private final ExpenseRepository repository;
    private final List<Expense> expenses = new ArrayList<>();
    private long nextId = 1;

    public ExpenseService(ExpenseRepository repository) throws IOException {
        this.repository = repository;
        expenses.addAll(repository.load());
        for (Expense expense : expenses) if (expense.getId() >= nextId) nextId = expense.getId() + 1;
    }

    public synchronized Expense add(String title, double amount, String category, String date) throws IOException {
        return add(title, amount, category, date, "Expense");
    }

    public synchronized Expense add(String title, double amount, String category, String date, String type) throws IOException {
        Expense expense = createExpense(nextId, title, amount, category, date, type);
        expenses.add(expense);
        nextId++;
        save();
        return expense;
    }

    private Expense createExpense(long id, String title, double amount, String category, String date, String type) {
        if ("Income".equalsIgnoreCase(type)) return new IncomeExpense(id, title, amount, category, date);
        return new PersonalExpense(id, title, amount, category, date);
    }

    public synchronized void remove(long id) throws ExpenseNotFoundException, IOException {
        int index = findIndex(id);
        expenses.remove(index);
        save();
    }

    public synchronized void edit(long id, String title, double amount, String category, String date, String type)
            throws ExpenseNotFoundException, IOException {
        int index = findIndex(id);
        Expense replacement = createExpense(id, title, amount, category, date, type);
        expenses.set(index, replacement);
        save();
    }

    public synchronized Expense find(long id) throws ExpenseNotFoundException {
        return expenses.get(findIndex(id));
    }

    private int findIndex(long id) throws ExpenseNotFoundException {
        for (int i = 0; i < expenses.size(); i++) if (expenses.get(i).getId() == id) return i;
        throw new ExpenseNotFoundException(id);
    }

    public synchronized List<Expense> all() { return new ArrayList<>(expenses); }

    public synchronized List<Expense> search(String text) {
        List<Expense> found = new ArrayList<>();
        String wanted = text.toLowerCase();
        for (Expense expense : expenses) {
            if (expense.getTitle().toLowerCase().contains(wanted) || expense.getCategory().toLowerCase().contains(wanted)) found.add(expense);
        }
        return found;
    }

    public synchronized Map<String, Double> totalsByCategory() {
        Map<String, Double> totals = new TreeMap<>();
        for (Expense expense : expenses) if (!"Income".equals(expense.getType())) addToMap(totals, expense.getCategory(), expense.getAmount());
        return totals;
    }

    public synchronized Map<String, Double> totalsByMonth() {
        Map<String, Double> totals = new TreeMap<>();
        for (Expense expense : expenses) if (!"Income".equals(expense.getType())) addToMap(totals, DateTools.monthKey(expense.getDate()), expense.getAmount());
        return totals;
    }

    public synchronized double total() {
        double total = 0;
        for (Expense expense : expenses) if (!"Income".equals(expense.getType())) total += expense.getAmount();
        return total;
    }

    public synchronized double totalIncome() {
        double total = 0;
        for (Expense expense : expenses) if ("Income".equals(expense.getType())) total += expense.getAmount();
        return total;
    }

    private void addToMap(Map<String, Double> totals, String key, double amount) {
        Double current = totals.get(key);
        totals.put(key, (current == null ? 0 : current) + amount);
    }

    private void save() throws IOException { repository.save(expenses); }
}
