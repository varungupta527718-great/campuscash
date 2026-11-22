package com.studentexpensetracker;

/** Income record kept in the same transaction model as expenses. */
public final class IncomeExpense extends Expense {
    public IncomeExpense(long id, String title, double amount, String category, String date) {
        super(id, title, amount, category, date);
    }
    @Override public String getType() { return "Income"; }
}
