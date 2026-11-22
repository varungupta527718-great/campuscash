package com.studentexpensetracker;

import java.io.Serializable;

/** A savings goal that can be saved to the settings file. */
public final class SavingsGoal implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private String name;
    private double target;
    private double saved;
    private String deadline;
    private String emoji;
    private boolean completed;

    public SavingsGoal(long id, String name, double target, double saved, String deadline, String emoji) {
        if (id < 1) throw new IllegalArgumentException("Goal ID must be positive.");
        this.id = id;
        update(name, target, saved, deadline, emoji);
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public double getTarget() { return target; }
    public double getSaved() { return saved; }
    public String getDeadline() { return deadline; }
    public String getEmoji() { return emoji; }
    public boolean isCompleted() { return completed; }

    public void update(String name, double target, double saved, String deadline, String emoji) {
        setName(name);
        setTarget(target);
        setSaved(saved);
        setDeadline(deadline);
        setEmoji(emoji);
    }

    public void contribute(double amount) {
        if (!MoneyTools.isPositive(amount)) throw new IllegalArgumentException("Contribution must be a valid positive amount.");
        setSaved(saved + amount);
    }

    private void setName(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Goal name is required.");
        name = value.trim();
    }
    private void setTarget(double value) {
        if (!MoneyTools.isPositive(value)) throw new IllegalArgumentException("Target must be a valid positive amount.");
        target = MoneyTools.round(value);
    }
    private void setSaved(double value) {
        if (!MoneyTools.isNonNegative(value)) throw new IllegalArgumentException("Saved amount must be valid and cannot be negative.");
        saved = MoneyTools.round(value);
        completed = saved >= target;
    }
    private void setDeadline(String value) {
        if (value != null && !value.isEmpty() && !DateTools.isValid(value)) throw new IllegalArgumentException("Deadline must use YYYY-MM-DD.");
        deadline = value == null ? "" : value;
    }
    private void setEmoji(String value) {
        emoji = value == null || value.trim().isEmpty() ? "\uD83C\uDFAF" : value;
    }
}
