package com.openfinance.payment.common.validator;

import java.util.regex.Pattern;

/**
 * Validator for Brazilian PIX keys
 * Supported types: EMAIL, CPF, CNPJ, PHONE, RANDOM
 */
public class PixKeyValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+55\\d{10,11}$"
    );

    private static final Pattern RANDOM_KEY_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );

    private PixKeyValidator() {
        // Utility class
    }

    public static boolean isValid(String pixKey, String pixKeyType) {
        if (pixKey == null || pixKey.isEmpty() || pixKeyType == null || pixKeyType.isEmpty()) {
            return false;
        }

        return switch (pixKeyType.toUpperCase()) {
            case "EMAIL" -> isValidEmail(pixKey);
            case "CPF" -> CPFValidator.isValid(pixKey);
            case "CNPJ" -> CNPJValidator.isValid(pixKey);
            case "PHONE" -> isValidPhone(pixKey);
            case "RANDOM" -> isValidRandomKey(pixKey);
            default -> false;
        };
    }

    private static boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private static boolean isValidPhone(String phone) {
        // Remove non-digits for validation
        String cleanPhone = phone.replaceAll("\\D", "");

        // Brazilian phone: +55 + DDD (2 digits) + number (8 or 9 digits)
        if (cleanPhone.length() >= 12 && cleanPhone.startsWith("55")) {
            return PHONE_PATTERN.matcher("+" + cleanPhone).matches();
        }

        // Also accept format without +
        return cleanPhone.length() >= 12 && cleanPhone.length() <= 13 && cleanPhone.startsWith("55");
    }

    private static boolean isValidRandomKey(String randomKey) {
        return RANDOM_KEY_PATTERN.matcher(randomKey.toLowerCase()).matches();
    }
}
