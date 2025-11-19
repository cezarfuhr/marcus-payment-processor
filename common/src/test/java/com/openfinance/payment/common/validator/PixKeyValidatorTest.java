package com.openfinance.payment.common.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PIX Key Validator Tests")
class PixKeyValidatorTest {

    @Test
    @DisplayName("Should validate EMAIL type PIX key")
    void shouldValidateEmailPixKey() {
        assertThat(PixKeyValidator.isValid("user@example.com", "EMAIL")).isTrue();
        assertThat(PixKeyValidator.isValid("test.user@domain.com.br", "EMAIL")).isTrue();
        assertThat(PixKeyValidator.isValid("invalid-email", "EMAIL")).isFalse();
        assertThat(PixKeyValidator.isValid("@example.com", "EMAIL")).isFalse();
    }

    @Test
    @DisplayName("Should validate CPF type PIX key")
    void shouldValidateCPFPixKey() {
        assertThat(PixKeyValidator.isValid("12345678909", "CPF")).isTrue();
        assertThat(PixKeyValidator.isValid("123.456.789-09", "CPF")).isTrue();
        assertThat(PixKeyValidator.isValid("12345678900", "CPF")).isFalse(); // Invalid check digit
    }

    @Test
    @DisplayName("Should validate CNPJ type PIX key")
    void shouldValidateCNPJPixKey() {
        assertThat(PixKeyValidator.isValid("11222333000181", "CNPJ")).isTrue();
        assertThat(PixKeyValidator.isValid("11.222.333/0001-81", "CNPJ")).isTrue();
        assertThat(PixKeyValidator.isValid("11222333000100", "CNPJ")).isFalse(); // Invalid check digit
    }

    @Test
    @DisplayName("Should validate PHONE type PIX key")
    void shouldValidatePhonePixKey() {
        assertThat(PixKeyValidator.isValid("+5511987654321", "PHONE")).isTrue();
        assertThat(PixKeyValidator.isValid("5511987654321", "PHONE")).isTrue();
        assertThat(PixKeyValidator.isValid("+551198765432", "PHONE")).isTrue(); // 10 digits
        assertThat(PixKeyValidator.isValid("+1234567890", "PHONE")).isFalse(); // Not Brazil
    }

    @Test
    @DisplayName("Should validate RANDOM type PIX key")
    void shouldValidateRandomPixKey() {
        assertThat(PixKeyValidator.isValid("123e4567-e89b-12d3-a456-426614174000", "RANDOM")).isTrue();
        assertThat(PixKeyValidator.isValid("550e8400-e29b-41d4-a716-446655440000", "RANDOM")).isTrue();
        assertThat(PixKeyValidator.isValid("not-a-uuid", "RANDOM")).isFalse();
        assertThat(PixKeyValidator.isValid("123456789", "RANDOM")).isFalse();
    }

    @Test
    @DisplayName("Should reject null or empty inputs")
    void shouldRejectNullOrEmpty() {
        assertThat(PixKeyValidator.isValid(null, "EMAIL")).isFalse();
        assertThat(PixKeyValidator.isValid("", "EMAIL")).isFalse();
        assertThat(PixKeyValidator.isValid("user@example.com", null)).isFalse();
        assertThat(PixKeyValidator.isValid("user@example.com", "")).isFalse();
    }

    @Test
    @DisplayName("Should reject invalid PIX key type")
    void shouldRejectInvalidPixKeyType() {
        assertThat(PixKeyValidator.isValid("user@example.com", "INVALID")).isFalse();
        assertThat(PixKeyValidator.isValid("12345678909", "UNKNOWN")).isFalse();
    }

    @Test
    @DisplayName("Should be case insensitive for PIX key type")
    void shouldBeCaseInsensitiveForType() {
        assertThat(PixKeyValidator.isValid("user@example.com", "email")).isTrue();
        assertThat(PixKeyValidator.isValid("user@example.com", "Email")).isTrue();
        assertThat(PixKeyValidator.isValid("12345678909", "cpf")).isTrue();
    }
}
