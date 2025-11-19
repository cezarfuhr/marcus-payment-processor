package com.openfinance.payment.common.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CPF Validator Tests")
class CPFValidatorTest {

    @Test
    @DisplayName("Should validate correct CPF")
    void shouldValidateCorrectCPF() {
        // Valid CPF: 123.456.789-09
        assertThat(CPFValidator.isValid("12345678909")).isTrue();

        // Valid CPF: 111.444.777-35
        assertThat(CPFValidator.isValid("11144477735")).isTrue();
    }

    @Test
    @DisplayName("Should reject CPF with wrong check digits")
    void shouldRejectInvalidCheckDigits() {
        assertThat(CPFValidator.isValid("12345678900")).isFalse();
        assertThat(CPFValidator.isValid("11144477700")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "22222222222", "99999999999"})
    @DisplayName("Should reject CPF with all same digits")
    void shouldRejectAllSameDigits(String cpf) {
        assertThat(CPFValidator.isValid(cpf)).isFalse();
    }

    @Test
    @DisplayName("Should reject CPF with wrong length")
    void shouldRejectWrongLength() {
        assertThat(CPFValidator.isValid("123456789")).isFalse();
        assertThat(CPFValidator.isValid("123456789012")).isFalse();
    }

    @Test
    @DisplayName("Should reject null or empty CPF")
    void shouldRejectNullOrEmpty() {
        assertThat(CPFValidator.isValid(null)).isFalse();
        assertThat(CPFValidator.isValid("")).isFalse();
    }

    @Test
    @DisplayName("Should validate CPF with formatting characters")
    void shouldValidateCPFWithFormatting() {
        // CPF with dots and dash: 123.456.789-09
        assertThat(CPFValidator.isValid("123.456.789-09")).isTrue();
    }

    @Test
    @DisplayName("Should reject CPF with letters")
    void shouldRejectCPFWithLetters() {
        assertThat(CPFValidator.isValid("1234567890A")).isFalse();
        assertThat(CPFValidator.isValid("ABC45678909")).isFalse();
    }
}
