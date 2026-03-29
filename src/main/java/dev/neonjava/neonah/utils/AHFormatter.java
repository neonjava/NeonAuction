package dev.neonjava.neonah.utils;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class AHFormatter {
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,###.##");

    public static String formatPrice(double price) {
        return PRICE_FORMAT.format(price) + "₹";
    }

    public static String formatTimeLeft(long listedAt) {
        long elapsed = System.currentTimeMillis() - listedAt;
        long limit = 32 * 60 * 60 * 1000L;
        long remaining = limit - elapsed;

        if (remaining <= 0)
            return "Expired";

        long hours = TimeUnit.MILLISECONDS.toHours(remaining);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;

        return hours + "h " + minutes + "m";
    }
}
