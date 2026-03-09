package com.ua.estore.cgsWeb.util;

import java.time.LocalDateTime;
import java.time.YearMonth;

public class TimeUtil {

    private TimeUtil() {}

    public static YearMonth getCurrentYearMonth() {
        return YearMonth.now();
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public static boolean isCardExpired(int year, int month) {
        return YearMonth.of(year, month).isBefore(getCurrentYearMonth());
    }
}
