package com.openfinance.payment.common.validator;

/**
 * Validator for Brazilian CNPJ (Cadastro Nacional da Pessoa Jurídica)
 * CNPJ format: 14 digits with 2 check digits
 */
public class CNPJValidator {

    private CNPJValidator() {
        // Utility class
    }

    public static boolean isValid(String cnpj) {
        if (cnpj == null || cnpj.isEmpty()) {
            return false;
        }

        // Remove non-digits
        String cleanCnpj = cnpj.replaceAll("\\D", "");

        // Check length
        if (cleanCnpj.length() != 14) {
            return false;
        }

        // Check for known invalid CNPJs (all same digits)
        if (cleanCnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        // Validate check digits
        try {
            // Calculate first check digit
            int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                sum += Character.getNumericValue(cleanCnpj.charAt(i)) * weights1[i];
            }
            int firstCheckDigit = sum % 11;
            firstCheckDigit = (firstCheckDigit < 2) ? 0 : 11 - firstCheckDigit;

            // Verify first check digit
            if (firstCheckDigit != Character.getNumericValue(cleanCnpj.charAt(12))) {
                return false;
            }

            // Calculate second check digit
            int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            sum = 0;
            for (int i = 0; i < 13; i++) {
                sum += Character.getNumericValue(cleanCnpj.charAt(i)) * weights2[i];
            }
            int secondCheckDigit = sum % 11;
            secondCheckDigit = (secondCheckDigit < 2) ? 0 : 11 - secondCheckDigit;

            // Verify second check digit
            return secondCheckDigit == Character.getNumericValue(cleanCnpj.charAt(13));

        } catch (Exception e) {
            return false;
        }
    }
}
