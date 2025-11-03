package com.huyin.inner_auction.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Simple password strength validator.
 * Rules:
 *  - minimum length (default 8)
 *  - at least one uppercase, one lowercase, one digit, one special char
 *  - not in common blacklist
 */
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    // minimal blacklist; expand as needed (common passwords)
    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
            "123456", "password", "12345678", "qwerty", "abc123", "password1"
    );

    public static boolean isStrong(String pw) {
        if (pw == null) return false;
        if (pw.length() < MIN_LENGTH) return false;
        if (!UPPER.matcher(pw).find()) return false;
        if (!LOWER.matcher(pw).find()) return false;
        if (!DIGIT.matcher(pw).find()) return false;
        if (!SPECIAL.matcher(pw).find()) return false;
        if (COMMON_PASSWORDS.contains(pw.toLowerCase())) return false;
        return true;
    }

    public static String explain(String pw) {
        if (pw == null) return "Password is null";
        if (pw.length() < MIN_LENGTH) return "Password must be at least " + MIN_LENGTH + " characters";
        if (!UPPER.matcher(pw).find()) return "Password must contain at least one uppercase letter";
        if (!LOWER.matcher(pw).find()) return "Password must contain at least one lowercase letter";
        if (!DIGIT.matcher(pw).find()) return "Password must contain at least one digit";
        if (!SPECIAL.matcher(pw).find()) return "Password must contain at least one special character (e.g. !@#$%)";
        if (COMMON_PASSWORDS.contains(pw.toLowerCase())) return "Password is too common";
        return "OK";
    }
}