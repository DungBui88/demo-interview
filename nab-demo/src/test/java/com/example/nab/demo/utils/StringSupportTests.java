package com.example.nab.demo.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringSupportTests {

    @Test
    void testIsValidPhoneNumberShouldReturnTrueWithValidPhoneNumber() {
        String validPhoneNumber = "1234567890";
        assertTrue(StringSupport.isValidPhoneNumber(validPhoneNumber));
    }

    @Test
    void testIsValidPhoneNumberShouldReturnFailWithInvalidPhoneNumber() {
        String invalidPhoneNumber = "a234567890";
        assertFalse(StringSupport.isValidPhoneNumber(invalidPhoneNumber));
        invalidPhoneNumber = "abcefdxzwa";
        assertFalse(StringSupport.isValidPhoneNumber(invalidPhoneNumber));
    }
}