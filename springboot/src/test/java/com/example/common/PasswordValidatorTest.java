package com.example.common;

import com.example.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    @Test
    void validPassword_passes() {
        assertDoesNotThrow(() -> PasswordValidator.validate("Abc@1234"));
    }

    @Test
    void validPassword_withSpecialChars() {
        assertDoesNotThrow(() -> PasswordValidator.validate("P@ssw0rd!"));
    }

    @Test
    void tooShort_throws() {
        CustomException ex = assertThrows(CustomException.class,
                () -> PasswordValidator.validate("Ab1@"));
        assertTrue(ex.getMessage().contains("8"));
    }

    @Test
    void nullPassword_throws() {
        assertThrows(CustomException.class, () -> PasswordValidator.validate(null));
    }

    @Test
    void emptyPassword_throws() {
        assertThrows(CustomException.class, () -> PasswordValidator.validate(""));
    }

    @Test
    void onlyLowercase_throws() {
        // 只有小写字母，1类字符，不满足3类要求
        assertThrows(CustomException.class, () -> PasswordValidator.validate("abcdefgh"));
    }

    @Test
    void onlyDigits_throws() {
        assertThrows(CustomException.class, () -> PasswordValidator.validate("12345678"));
    }

    @Test
    void twoCategories_throws() {
        // 大写+小写 = 2类，不满足3类
        assertThrows(CustomException.class, () -> PasswordValidator.validate("Abcdefgh"));
    }

    @Test
    void threeCategories_passes() {
        // 大写+小写+数字 = 3类
        assertDoesNotThrow(() -> PasswordValidator.validate("Abcdefg1"));
    }

    @Test
    void fourCategories_passes() {
        // 大写+小写+数字+特殊 = 4类
        assertDoesNotThrow(() -> PasswordValidator.validate("Abc1@xyz"));
    }

    @Test
    void tooLong_throws() {
        String longPassword = "A".repeat(30) + "b".repeat(30) + "1".repeat(10);
        assertThrows(CustomException.class, () -> PasswordValidator.validate(longPassword));
    }
}
