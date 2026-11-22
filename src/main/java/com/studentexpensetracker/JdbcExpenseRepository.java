package com.studentexpensetracker;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Optional database storage using the JDBC topics from the course. */
public final class JdbcExpenseRepository implements ExpenseRepository {
    private final Connection connection;

    public JdbcExpenseRepository(String url, String user, String password) throws SQLException {
        if (user == null || user.trim().isEmpty()) connection = DriverManager.getConnection(url);
        else connection = DriverManager.getConnection(url, user, password == null ? "" : password);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS expenses (id BIGINT PRIMARY KEY, title VARCHAR(200) NOT NULL, amount DECIMAL(12,2) NOT NULL, category VARCHAR(80) NOT NULL, expense_date VARCHAR(10) NOT NULL, type VARCHAR(30) NOT NULL)");
        }
    }

    @Override public List<Expense> load() throws IOException {
        List<Expense> expenses = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id,title,amount,category,expense_date,type FROM expenses ORDER BY id")) {
            while (rows.next()) {
                long id = rows.getLong(1);
                String title = rows.getString(2);
                double amount = rows.getDouble(3);
                String category = rows.getString(4);
                String date = rows.getString(5);
                String type = rows.getString(6);
                if ("Income".equalsIgnoreCase(type)) expenses.add(new IncomeExpense(id, title, amount, category, date));
                else expenses.add(new PersonalExpense(id, title, amount, category, date));
            }
            return expenses;
        } catch (SQLException | RuntimeException e) { throw new IOException("JDBC read failed.", e); }
    }

    @Override public void save(List<Expense> expenses) throws IOException {
        try {
            connection.setAutoCommit(false);
            try (Statement clear = connection.createStatement()) { clear.executeUpdate("DELETE FROM expenses"); }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO expenses(id,title,amount,category,expense_date,type) VALUES(?,?,?,?,?,?)")) {
                for (Expense expense : expenses) {
                    statement.setLong(1, expense.getId());
                    statement.setString(2, expense.getTitle());
                    statement.setDouble(3, expense.getAmount());
                    statement.setString(4, expense.getCategory());
                    statement.setString(5, expense.getDate());
                    statement.setString(6, expense.getType());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            throw new IOException("JDBC write failed.", e);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) { }
        }
    }

    @Override public void close() throws SQLException { connection.close(); }
}
