package com.studentexpensetracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes CSV records with the character-stream classes from class. */
public final class CsvExpenseRepository implements ExpenseRepository {
    private final File file;

    public CsvExpenseRepository(String fileName) { file = new File(fileName); }

    @Override public List<Expense> load() throws IOException {
        List<Expense> expenses = new ArrayList<>();
        if (!file.exists()) return expenses;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // Skip the column headings.
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) expenses.add(readExpense(line));
            }
        }
        return expenses;
    }

    private Expense readExpense(String line) throws IOException {
        String[] columns = parseCsvLine(line);
        if (columns.length < 5) throw new IOException("Invalid CSV row: " + line);
        try {
            long id = Long.parseLong(columns[0]);
            String title = columns[1];
            double amount = Double.parseDouble(columns[2]);
            String category = columns[3];
            String date = columns[4];
            if (columns.length > 5 && "Income".equalsIgnoreCase(columns[5])) {
                return new IncomeExpense(id, title, amount, category, date);
            }
            return new PersonalExpense(id, title, amount, category, date);
        } catch (RuntimeException e) {
            throw new IOException("Could not read expense row: " + line, e);
        }
    }

    @Override public void save(List<Expense> expenses) throws IOException {
        createParentFolder();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("id,title,amount,category,date,type");
            writer.newLine();
            for (Expense expense : expenses) writeExpense(writer, expense);
        }
    }

    private void createParentFolder() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create data folder.");
    }

    private void writeExpense(BufferedWriter writer, Expense expense) throws IOException {
        writer.write(expense.getId() + "," + quote(expense.getTitle()) + "," + MoneyTools.format(expense.getAmount()) + ","
                + quote(expense.getCategory()) + "," + expense.getDate() + "," + expense.getType());
        writer.newLine();
    }

    private static String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    private static String[] parseCsvLine(String line) throws IOException {
        List<String> columns = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean insideQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"' && insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                value.append('"');
                i++;
            } else if (current == '"') {
                insideQuotes = !insideQuotes;
            } else if (current == ',' && !insideQuotes) {
                columns.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (insideQuotes) throw new IOException("CSV row has an unclosed quote.");
        columns.add(value.toString());
        return columns.toArray(new String[0]);
    }
}
