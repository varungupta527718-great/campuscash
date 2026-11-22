package com.studentexpensetracker;

public final class PersonalExpense extends Expense {
    public PersonalExpense(long id, String title, double amount, String category, String date) {
        super(id, title, amount, category, date);
    }
    @Override public String getType() { return "Personal"; }
}
