package com.studentexpensetracker;

/** Small amount-validation and display methods used by the tracker. */
public final class MoneyTools {
    private static final double MAX_AMOUNT = 9000000000.00;
    private MoneyTools() { }

    public static boolean isPositive(double amount) {
        return amount > 0 && amount <= MAX_AMOUNT && !Double.isInfinite(amount) && !Double.isNaN(amount);
    }
    public static boolean isNonNegative(double amount) {
        return amount >= 0 && amount <= MAX_AMOUNT && !Double.isInfinite(amount) && !Double.isNaN(amount);
    }
    public static double round(double amount) {
        if (amount < 0) return -round(-amount);
        return (long) (amount * 100 + 0.5) / 100.0;
    }
    public static String format(double amount) {
        boolean negative = amount < 0;
        double positiveAmount = negative ? -amount : amount;
        long centsTotal = (long) (positiveAmount * 100 + 0.5);
        long whole = centsTotal / 100;
        long cents = centsTotal % 100;
        return (negative ? "-" : "") + whole + "." + (cents < 10 ? "0" : "") + cents;
    }
}
