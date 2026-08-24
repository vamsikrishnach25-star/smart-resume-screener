package com.vamsi.screener.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Small text helpers shared by the parsing and scoring layers. */
public final class TextUtils {

    private TextUtils() {
        // utility class
    }

    /**
     * Normalises text pulled out of a PDF: collapses runs of spaces, trims every line and removes
     * runs of blank lines, so prompts stay compact and heuristics see predictable input.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String unified = raw.replace("\r\n", "\n").replace('\r', '\n').replace('\u00A0', ' ');
        String[] lines = unified.split("\n", -1);
        StringBuilder out = new StringBuilder();
        int blankRun = 0;
        for (String line : lines) {
            String cleaned = line.replaceAll("[\\t\\x0B\\f]+", " ").replaceAll(" {2,}", " ").trim();
            if (cleaned.isEmpty()) {
                blankRun++;
                if (blankRun > 1) {
                    continue;
                }
            } else {
                blankRun = 0;
            }
            out.append(cleaned).append('\n');
        }
        return out.toString().trim();
    }

    /** Shortens {@code text} to {@code maxChars}, appending a marker when content was dropped. */
    public static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (maxChars <= 0 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n...[truncated]";
    }

    /** @return lowercase hex SHA-256 of the given text, used to de-duplicate uploads. */
    public static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** @return the trimmed value, or null when the value is null/blank. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Rounds a score to one decimal place and clamps it into the 1-10 band. */
    public static double clampScore(double value) {
        double bounded = Math.max(1.0D, Math.min(10.0D, value));
        return Math.round(bounded * 10.0D) / 10.0D;
    }
}
