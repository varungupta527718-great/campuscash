package com.studentexpensetracker;

/** Date methods for the YYYY-MM-DD format used by the tracker. */
public final class DateTools {
    private static final String[] MONTHS = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };
    private static final long INDIA_TIME_OFFSET = 19800000L;
    private static final long MILLISECONDS_IN_DAY = 86400000L;

    private DateTools() { }

    public static String today() {
        // Convert the number of days since 1970 into a calendar date.
        long days = (System.currentTimeMillis() + INDIA_TIME_OFFSET) / MILLISECONDS_IN_DAY;
        int year = 1970;
        while (days >= daysInYear(year)) {
            days -= daysInYear(year);
            year++;
        }
        int month = 1;
        while (days >= daysInMonth(year, month)) {
            days -= daysInMonth(year, month);
            month++;
        }
        int day = (int) days + 1;
        return fourDigits(year) + "-" + twoDigits(month) + "-" + twoDigits(day);
    }

    public static boolean isValid(String value) {
        if (value == null || value.length() != 10) return false;
        if (value.charAt(4) != '-' || value.charAt(7) != '-') return false;
        for (int i = 0; i < value.length(); i++) {
            if (i == 4 || i == 7) continue;
            if (value.charAt(i) < '0' || value.charAt(i) > '9') return false;
        }
        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(5, 7));
        int day = Integer.parseInt(value.substring(8, 10));
        return year >= 1900 && month >= 1 && month <= 12 && day >= 1 && day <= daysInMonth(year, month);
    }

    public static int year(String value) { return Integer.parseInt(value.substring(0, 4)); }
    public static int month(String value) { return Integer.parseInt(value.substring(5, 7)); }
    public static int day(String value) { return Integer.parseInt(value.substring(8, 10)); }
    public static String monthKey(String value) { return value.substring(0, 7); }
    public static String monthName(String value) { return MONTHS[month(value) - 1]; }

    public static String addDays(String value, int daysToAdd) {
        int year = year(value);
        int month = month(value);
        int day = day(value);
        while (daysToAdd > 0) {
            day++;
            if (day > daysInMonth(year, month)) {
                day = 1;
                month++;
                if (month > 12) { month = 1; year++; }
            }
            daysToAdd--;
        }
        while (daysToAdd < 0) {
            day--;
            if (day < 1) {
                month--;
                if (month < 1) { month = 12; year--; }
                day = daysInMonth(year, month);
            }
            daysToAdd++;
        }
        return fourDigits(year) + "-" + twoDigits(month) + "-" + twoDigits(day);
    }

    public static int daysBetween(String from, String to) {
        return (int) (daysSinceYearZero(to) - daysSinceYearZero(from));
    }

    private static long daysSinceYearZero(String value) {
        int year = year(value);
        int month = month(value);
        int day = day(value);
        long total = (long) year * 365 + leapYearsBefore(year);
        for (int currentMonth = 1; currentMonth < month; currentMonth++) total += daysInMonth(year, currentMonth);
        return total + day - 1;
    }

    public static int daysInMonth(int year, int month) {
        if (month == 2) return isLeapYear(year) ? 29 : 28;
        if (month == 4 || month == 6 || month == 9 || month == 11) return 30;
        return 31;
    }

    private static int daysInYear(int year) { return isLeapYear(year) ? 366 : 365; }
    private static boolean isLeapYear(int year) { return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0); }
    private static long leapYearsBefore(int year) { return (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400; }
    private static String twoDigits(int number) { return number < 10 ? "0" + number : "" + number; }
    private static String fourDigits(int number) {
        if (number < 10) return "000" + number;
        if (number < 100) return "00" + number;
        if (number < 1000) return "0" + number;
        return "" + number;
    }
}
