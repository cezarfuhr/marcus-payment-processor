package com.openfinance.payment.common.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CNPJ Validator Tests")
class CNPJValidatorTest {

    @Test
    @DisplayName("Should validate correct CNPJ")
    void shouldValidateCorrectCNPJ() {
        // Valid CNPJ: 11.222.333/0001-81
        assertThat(CNPJValidator.isValid("11222333000181")).isTrue();

        // Valid CNPJ: 12.345.678/0001-95
        assertThat(CNPJValidator.isValid("12345678000195")).isTrue();
    }

    @Test
    @DisplayName("Should reject CNPJ with wrong check digits")
    void shouldRejectInvalidCheckDigits() {
        assertThat(CNPJValidator.isValid("11222333000100")).isFalse();
        assertThat(CNPJValidator.isValid("12345678000100")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000000", "11111111111111", "22222222222222"})
    @DisplayName("Should reject CNPJ with all same digits")
    void shouldRejectAllSameDigits(String cnpj) {
        assertThat(CNPJValidator.isValid(cnpj)).isFalse();
    }

    @Test
    @DisplayName("Should reject CNPJ with wrong length")
    void shouldRejectWrongLength() {
        assertThat(CNPJValidator.isValid("1122233300018")).isFalse();
        assertThat(CNPJValidator.isValid("112223330001811")).isFalse();
    }

    @Test
    @DisplayName("Should reject null or empty CNPJ")
    void shouldRejectNullOrEmpty() {
        assertThat(CNPJValidator.isValid(null)).isFalse();
        assertThat(CNPJValidator.isValid("")).isFalse();
    }

    @Test
    @DisplayName("Should validate CNPJ with formatting characters")
    void shouldValidateCNPJWithFormatting() {
        // CNPJ with dots, slash and dash: 11.222.333/0001-81
        assertThat(CNPJValidator.isValid("11.222.333/0001-81")).isTrue();
    }

    @Test
    @DisplayName("Should reject CNPJ with letters")
    void shouldRejectCNPJWithLetters() {
        assertThat(CNPJValidator.isValid("1122233300018A")).isFalse();
        assertThat(CNPJValidator.isValid("ABC22333000181")).isFalse();
    }
}
