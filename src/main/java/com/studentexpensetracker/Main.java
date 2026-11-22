package com.studentexpensetracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/** Console version of the Student Expense Tracker. */
public final class Main {
    private static final Scanner INPUT = new Scanner(System.in);
    private Main() { }

    public static void main(String[] args) {
        ExpenseRepository repository = null;
        try {
            String jdbc = System.getenv("EXPENSE_JDBC_URL");
            if (jdbc != null && !jdbc.trim().isEmpty()) {
                repository = new JdbcExpenseRepository(jdbc, System.getenv("EXPENSE_JDBC_USER"), System.getenv("EXPENSE_JDBC_PASSWORD"));
                System.out.println("Storage: JDBC");
            } else {
                repository = new CsvExpenseRepository("data/expenses.csv");
                System.out.println("Storage: data/expenses.csv");
            }
            runMenu(new ExpenseService(repository));
        } catch (Exception e) {
            System.out.println("Could not start/save the tracker: " + e.getMessage());
        } finally {
            if (repository != null) try { repository.close(); }
            catch (Exception e) { System.out.println("Could not close storage: " + e.getMessage()); }
        }
    }

    private static void runMenu(ExpenseService service) {
        boolean running = true;
        while (running) {
            System.out.println("\n=== Student Expense Tracker ===\n1 Add expense/income\n2 List transactions\n3 Search\n4 Edit\n5 Delete\n6 Summary\n7 Sort transactions\n8 Export CSV\n9 Import CSV\n10 Concurrent report demo\n11 Java course concepts workshop\n0 Exit");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1: add(service); break;
                case 2: list(service.all()); break;
                case 3: list(service.search(readText("Search title/category: "))); break;
                case 4: edit(service); break;
                case 5: delete(service); break;
                case 6: summary(service); break;
                case 7: sort(service); break;
                case 8: exportCsv(service); break;
                case 9: importCsv(service); break;
                case 10: concurrentReport(service); break;
                case 11: JavaConceptWorkshop.run(service.all()); break;
                case 0: running = false; break;
                default: System.out.println("Choose a number from 0 to 11.");
            }
        }
        System.out.println("Goodbye!");
    }

    private static void add(ExpenseService service) {
        try {
            String type = readText("Type (expense/income): ");
            Expense expense = service.add(readText("Title/source: "), readAmount("Amount: "), readText("Category: "),
                    readDate("Date (YYYY-MM-DD, blank=today): "), "income".equalsIgnoreCase(type) ? "Income" : "Expense");
            System.out.println("Added: " + expense);
        } catch (IOException | IllegalArgumentException e) { System.out.println("Could not add transaction: " + e.getMessage()); }
    }

    private static void edit(ExpenseService service) {
        try {
            long id = readLong("Transaction ID: ");
            Expense old = service.find(id);
            System.out.println("Editing " + old);
            service.edit(id, readText("New title: "), readAmount("New amount: "), readText("New category: "),
                    readDate("New date (YYYY-MM-DD): "), old.getType());
            System.out.println("Updated.");
        } catch (Exception e) { System.out.println("Could not edit: " + e.getMessage()); }
    }

    private static void delete(ExpenseService service) {
        try { service.remove(readLong("Transaction ID to delete: ")); System.out.println("Deleted."); }
        catch (Exception e) { System.out.println("Could not delete: " + e.getMessage()); }
    }

    private static void list(List<Expense> rows) {
        if (rows.isEmpty()) { System.out.println("No transactions found."); return; }
        for (Expense expense : rows) System.out.println(expense);
    }

    private static void summary(ExpenseService service) {
        double spending = service.total();
        double income = service.totalIncome();
        System.out.println("Total spent: " + money(spending) + " | Income: " + money(income) + " | Net: " + money(income - spending));
        System.out.println("By category:");
        for (Map.Entry<String, Double> row : service.totalsByCategory().entrySet()) System.out.println("  " + row.getKey() + ": " + money(row.getValue()));
        System.out.println("By month:");
        for (Map.Entry<String, Double> row : service.totalsByMonth().entrySet()) System.out.println("  " + row.getKey() + ": " + money(row.getValue()));

        String[] categories = service.totalsByCategory().keySet().toArray(new String[0]);
        double[][] monthCategory = new double[12][categories.length];
        String currentYear = DateTools.today().substring(0, 4);
        for (Expense expense : service.all()) {
            if (!"Income".equals(expense.getType()) && expense.getDate().startsWith(currentYear)) {
                int categoryIndex = indexOf(categories, expense.getCategory());
                if (categoryIndex >= 0) monthCategory[DateTools.month(expense.getDate()) - 1][categoryIndex] += expense.getAmount();
            }
        }
        System.out.println("Current-year month/category array prepared: " + monthCategory.length + " x " + categories.length);
    }

    private static int indexOf(String[] values, String wanted) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(wanted)) return i;
        return -1;
    }
    private static String money(double value) { return MoneyTools.format(value); }

    private static void sort(ExpenseService service) {
        List<Expense> rows = new ArrayList<>(service.all());
        System.out.println("Sort by: 1 date (Comparable), 2 amount (Comparator)");
        if (readInt("Choose: ") == 1) Collections.sort(rows);
        else Collections.sort(rows, new Comparator<Expense>() {
            public int compare(Expense left, Expense right) {
                if (left.getAmount() > right.getAmount()) return -1;
                if (left.getAmount() < right.getAmount()) return 1;
                return left.getTitle().compareToIgnoreCase(right.getTitle());
            }
        });
        list(rows);
    }

    private static void exportCsv(ExpenseService service) {
        String name = readText("Output CSV path: ");
        try { new CsvExpenseRepository(name).save(service.all()); System.out.println("Exported " + service.all().size() + " rows."); }
        catch (IOException e) { System.out.println("Export failed: " + e.getMessage()); }
    }

    private static void importCsv(ExpenseService service) {
        String name = readText("Input CSV path: ");
        try {
            List<Expense> incoming = new CsvExpenseRepository(name).load();
            Set<String> known = new HashSet<>();
            for (Expense expense : service.all()) known.add(makeDuplicateKey(expense));
            int added = 0;
            for (Expense expense : incoming) {
                String key = makeDuplicateKey(expense);
                if (!known.contains(key)) {
                    service.add(expense.getTitle(), expense.getAmount(), expense.getCategory(), expense.getDate(), expense.getType());
                    known.add(key);
                    added++;
                }
            }
            System.out.println("Imported " + added + " new rows (duplicates skipped).");
        } catch (Exception e) { System.out.println("Import failed: " + e.getMessage()); }
    }
    private static String makeDuplicateKey(Expense expense) {
        return expense.getType() + "|" + expense.getDate() + "|" + expense.getTitle() + "|" + expense.getAmount();
    }

    private static void concurrentReport(ExpenseService service) {
        Thread report = new Thread(new Runnable() {
            public void run() { System.out.println("Background total: " + money(service.total()) + " across " + service.all().size() + " transactions"); }
        }, "expense-report-worker");
        report.start();
        try { report.join(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); System.out.println("Report interrupted."); }
    }

    private static String readText(String prompt) { System.out.print(prompt); return INPUT.nextLine().trim(); }
    private static int readInt(String prompt) {
        while (true) try { return Integer.parseInt(readText(prompt)); }
        catch (NumberFormatException e) { System.out.println("Enter a whole number."); }
    }
    private static long readLong(String prompt) {
        while (true) try { return Long.parseLong(readText(prompt)); }
        catch (NumberFormatException e) { System.out.println("Enter a valid ID."); }
    }
    private static double readAmount(String prompt) {
        while (true) try {
            double amount = Double.parseDouble(readText(prompt));
            if (!MoneyTools.isPositive(amount)) throw new NumberFormatException();
            return amount;
        } catch (NumberFormatException e) { System.out.println("Enter an amount greater than zero."); }
    }
    private static String readDate(String prompt) {
        while (true) {
            String value = readText(prompt);
            if (value.isEmpty() && prompt.contains("blank=today")) return DateTools.today();
            if (DateTools.isValid(value)) return value;
            System.out.println("Use a real date in YYYY-MM-DD format.");
        }
    }
}
