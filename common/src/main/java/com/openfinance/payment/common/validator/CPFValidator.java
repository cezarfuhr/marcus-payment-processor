package com.openfinance.payment.common.validator;

/**
 * Validator for Brazilian CPF (Cadastro de Pessoas Físicas)
 * CPF format: 11 digits with 2 check digits
 */
public class CPFValidator {

    private CPFValidator() {
        // Utility class
    }

    public static boolean isValid(String cpf) {
        if (cpf == null || cpf.isEmpty()) {
            return false;
        }

        // Remove non-digits
        String cleanCpf = cpf.replaceAll("\\D", "");

        // Check length
        if (cleanCpf.length() != 11) {
            return false;
        }

        // Check for known invalid CPFs (all same digits)
        if (cleanCpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        // Validate check digits
        try {
            // Calculate first check digit
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += Character.getNumericValue(cleanCpf.charAt(i)) * (10 - i);
            }
            int firstCheckDigit = 11 - (sum % 11);
            if (firstCheckDigit >= 10) {
                firstCheckDigit = 0;
            }

            // Verify first check digit
            if (firstCheckDigit != Character.getNumericValue(cleanCpf.charAt(9))) {
                return false;
            }

            // Calculate second check digit
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += Character.getNumericValue(cleanCpf.charAt(i)) * (11 - i);
            }
            int secondCheckDigit = 11 - (sum % 11);
            if (secondCheckDigit >= 10) {
                secondCheckDigit = 0;
            }

            // Verify second check digit
            return secondCheckDigit == Character.getNumericValue(cleanCpf.charAt(10));

        } catch (Exception e) {
            return false;
        }
    }
}
