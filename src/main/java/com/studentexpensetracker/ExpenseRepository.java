package com.studentexpensetracker;

import java.io.IOException;
import java.util.List;

public interface ExpenseRepository {
    List<Expense> load() throws IOException;
    void save(List<Expense> expenses) throws IOException;
    default void close() throws Exception { }
}
