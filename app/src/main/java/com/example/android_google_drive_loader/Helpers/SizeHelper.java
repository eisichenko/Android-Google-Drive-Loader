package com.example.android_google_drive_loader.Helpers;

import java.text.DecimalFormat;

public class SizeHelper {
    private static final long K = 1024;
    private static final long M = K * K;
    private static final long G = M * K;
    private static final long T = G * K;

    public static String convertToStringRepresentation(final long value){
        final long[] dividers = new long[] { T, G, M, K, 1 };
        final String[] units = new String[] { "TB", "GB", "MB", "KB", "B" };

        if (value < 0)
            throw new IllegalArgumentException("Invalid file size: " + value);

        if (value == 0L) {
            return "0 B";
        }

        String result = null;

        for (int i = 0; i < dividers.length; i++) {
            final long divider = dividers[i];
            if (value >= divider) {
                result = format(value, divider, units[i]);
                break;
            }
        }

        return result;
    }

    private static String format(final long value, final long divider, final String unit){
        final double result =
                divider > 1 ? (double) value / (double) divider : (double) value;
        return new DecimalFormat("#,##0.#").format(result) + " " + unit;
    }
}
