package com.studentexpensetracker;

public abstract class Expense implements Comparable<Expense>, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private static final String RECORD_VERSION;
    static { RECORD_VERSION = "1"; }
    private final long id;
    private String title;
    private double amount;
    private String category;
    private String date;

    protected Expense(long id, String title, double amount, String category, String date) {
        if (id < 1) throw new IllegalArgumentException("ID must be positive.");
        this.id = id;
        setTitle(title);
        setAmount(amount);
        setCategory(category);
        setDate(date);
    }
    public long getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title cannot be empty.");
        this.title = title.trim();
    }
    public void setAmount(double amount) {
        if (!MoneyTools.isPositive(amount)) throw new IllegalArgumentException("Amount must be between zero and 9,000,000,000.");
        this.amount = MoneyTools.round(amount);
    }
    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) throw new IllegalArgumentException("Category cannot be empty.");
        this.category = category.trim();
    }
    public void setDate(String date) {
        if (!DateTools.isValid(date)) throw new IllegalArgumentException("Date must use YYYY-MM-DD.");
        this.date = date;
    }
    public abstract String getType();
    public static String recordVersion() { return RECORD_VERSION; }
    @Override public int compareTo(Expense other) {
        int byDate = date.compareTo(other.date);
        return byDate != 0 ? byDate : (id < other.id ? -1 : id == other.id ? 0 : 1);
    }
    @Override public String toString() {
        return new StringBuilder().append('#').append(id).append(" | ").append(date).append(" | ")
                .append(padRight(getType(), 12)).append(" | ").append(padRight(title, 18))
                .append(" | ").append(padRight(category, 14)).append(" | INR ").append(MoneyTools.format(amount)).toString();
    }

    private String padRight(String text, int width) {
        StringBuilder result = new StringBuilder(text);
        while (result.length() < width) result.append(' ');
        return result.toString();
    }
}
