package com.studentexpensetracker;

public class ExpenseNotFoundException extends Exception {
    public ExpenseNotFoundException(long id) { super("No expense found with ID " + id + "."); }
}
