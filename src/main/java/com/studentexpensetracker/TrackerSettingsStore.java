package com.studentexpensetracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Java file storage for the profile, budgets, and savings goals. */
public final class TrackerSettingsStore {
    private final File file;
    private Map<String, String> profile = new LinkedHashMap<>();
    private Map<String, Double> budgets = new LinkedHashMap<>();
    private List<SavingsGoal> goals = new ArrayList<>();
    private long nextGoalId = 1;

    public TrackerSettingsStore(String fileName) {
        file = new File(fileName);
        load();
    }

    private synchronized void load() {
        if (!file.exists()) return;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            Object saved = input.readObject();
            if (!(saved instanceof Map<?, ?>)) return;
            Map<?, ?> savedValues = (Map<?, ?>) saved;
            loadProfile(savedValues.get("profile"));
            loadBudgets(savedValues.get("budgets"));
            loadGoals(savedValues.get("goals"));
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            System.err.println("Could not read settings file: " + e.getMessage());
        }
    }

    private void loadProfile(Object savedProfile) {
        if (!(savedProfile instanceof Map<?, ?>)) return;
        Map<?, ?> values = (Map<?, ?>) savedProfile;
        for (Map.Entry<?, ?> row : values.entrySet()) {
            if (row.getKey() instanceof String && row.getValue() instanceof String) {
                profile.put((String) row.getKey(), (String) row.getValue());
            }
        }
    }

    private void loadBudgets(Object savedBudgets) {
        if (!(savedBudgets instanceof Map<?, ?>)) return;
        Map<?, ?> values = (Map<?, ?>) savedBudgets;
        for (Map.Entry<?, ?> row : values.entrySet()) {
            if (row.getKey() instanceof String && row.getValue() instanceof Number) {
                budgets.put((String) row.getKey(), ((Number) row.getValue()).doubleValue());
            }
        }
    }

    private void loadGoals(Object savedGoals) {
        if (!(savedGoals instanceof List<?>)) return;
        List<?> values = (List<?>) savedGoals;
        for (Object value : values) {
            if (value instanceof SavingsGoal) {
                SavingsGoal goal = (SavingsGoal) value;
                goals.add(goal);
                if (goal.getId() >= nextGoalId) nextGoalId = goal.getId() + 1;
            }
        }
    }

    public synchronized Map<String, String> profile() { return new LinkedHashMap<>(profile); }
    public synchronized void updateProfile(Map<String, String> values) throws IOException {
        profile.putAll(values);
        save();
    }
    public synchronized Map<String, Double> budgets() { return new LinkedHashMap<>(budgets); }

    public synchronized void setBudget(String category, double amount) throws IOException {
        if (category == null || category.trim().isEmpty()) throw new IllegalArgumentException("Category required.");
        if (!MoneyTools.isNonNegative(amount)) throw new IllegalArgumentException("Budget must be valid and cannot be negative.");
        if (amount == 0) budgets.remove(category);
        else budgets.put(category, MoneyTools.round(amount));
        save();
    }

    public synchronized List<SavingsGoal> goals() { return new ArrayList<>(goals); }

    public synchronized SavingsGoal addGoal(String name, double target, double saved, String deadline, String emoji) throws IOException {
        SavingsGoal goal = new SavingsGoal(nextGoalId, name, target, saved, deadline, emoji);
        goals.add(goal);
        nextGoalId++;
        save();
        return goal;
    }

    public synchronized SavingsGoal updateGoal(long id, String name, double target, double saved, String deadline, String emoji) throws IOException {
        SavingsGoal goal = findGoal(id);
        goal.update(name, target, saved, deadline, emoji);
        save();
        return goal;
    }

    public synchronized void removeGoal(long id) throws IOException {
        for (int i = 0; i < goals.size(); i++) {
            if (goals.get(i).getId() == id) {
                goals.remove(i);
                save();
                return;
            }
        }
        throw new NoSuchElementException("Goal not found.");
    }

    public synchronized SavingsGoal contribute(long id, double amount) throws IOException {
        SavingsGoal goal = findGoal(id);
        goal.contribute(amount);
        save();
        return goal;
    }

    private SavingsGoal findGoal(long id) {
        for (SavingsGoal goal : goals) if (goal.getId() == id) return goal;
        throw new NoSuchElementException("Goal not found.");
    }

    private void save() throws IOException {
        File folder = file.getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs()) throw new IOException("Could not create data folder.");
        Map<String, Object> values = new HashMap<>();
        values.put("profile", profile);
        values.put("budgets", budgets);
        values.put("goals", goals);
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(values);
        }
    }
}
