package dev.neonjava.neonah.utils;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class AHFormatter {
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,###.##");

    public static String formatPrice(double price) {
        if (price >= 1_000_000_000)
            return formatCompact(price / 1_000_000_000, "b");
        if (price >= 1_000_000)
            return formatCompact(price / 1_000_000, "m");
        if (price >= 1_000)
            return formatCompact(price / 1_000, "k");
        return PRICE_FORMAT.format(price) + "₹";
    }

    public static double parsePrice(String input) {
        input = input.toLowerCase().trim();
        double multiplier = 1.0;

        if (input.endsWith("k")) {
            multiplier = 1_000.0;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = 1_000_000.0;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            input = input.substring(0, input.length() - 1);
        }

        try {
            return Double.parseDouble(input) * multiplier;
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private static String formatCompact(double val, String suffix) {
        return (val == (long) val ? String.format("%d", (long) val) : String.format("%.1f", val)) + suffix + "₹";
    }

    public static String stripBold(String text) {
        if (text == null)
            return null;
        return text.replace("§l", "").replace("§L", "");
    }

    public static String toSmallCaps(String text) {
        if (text == null)
            return "";
        char[] chars = text.toLowerCase().toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            switch (c) {
                case 'a' -> sb.append('ᴀ');
                case 'b' -> sb.append('ʙ');
                case 'c' -> sb.append('ᴄ');
                case 'd' -> sb.append('ᴅ');
                case 'e' -> sb.append('ᴇ');
                case 'f' -> sb.append('ꜰ');
                case 'g' -> sb.append('ɢ');
                case 'h' -> sb.append('ʜ');
                case 'i' -> sb.append('ɪ');
                case 'j' -> sb.append('ᴊ');
                case 'k' -> sb.append('ᴋ');
                case 'l' -> sb.append('ʟ');
                case 'm' -> sb.append('ᴍ');
                case 'n' -> sb.append('ɴ');
                case 'o' -> sb.append('ᴏ');
                case 'p' -> sb.append('ᴘ');
                case 'q' -> sb.append('ǫ');
                case 'r' -> sb.append('ʀ');
                case 's' -> sb.append('ѕ');
                case 't' -> sb.append('ᴛ');
                case 'u' -> sb.append('ᴜ');
                case 'v' -> sb.append('ᴠ');
                case 'w' -> sb.append('ᴡ');
                case 'x' -> sb.append('x');
                case 'y' -> sb.append('ʏ');
                case 'z' -> sb.append('ᴢ');
                default -> sb.append(c);
            }
        }
        return sb.toString();
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
