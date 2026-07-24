package com.example.common;

import com.example.common.enums.ResultCodeEnum;
import com.example.exception.CustomException;

/**
 * 密码复杂度校验工具
 * <p>
 * 规则：最少 8 位，必须包含大写字母、小写字母、数字、特殊字符中的至少 3 类
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    // 至少满足 3 类字符（大小写 + 数字 + 特殊字符 中的 3 种）
    private static final int MIN_CATEGORIES = 3;

    private PasswordValidator() {}

    /**
     * 校验密码复杂度，不满足则抛出异常
     */
    public static void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new CustomException(ResultCodeEnum.PASSWORD_TOO_WEAK);
        }
        if (password.length() < MIN_LENGTH) {
            throw new CustomException("5006", "密码长度不能少于 " + MIN_LENGTH + " 位");
        }
        if (password.length() > MAX_LENGTH) {
            throw new CustomException("5006", "密码长度不能超过 " + MAX_LENGTH + " 位");
        }

        int categories = 0;
        if (containsUpperCase(password)) categories++;
        if (containsLowerCase(password)) categories++;
        if (containsDigit(password)) categories++;
        if (containsSpecialChar(password)) categories++;

        if (categories < MIN_CATEGORIES) {
            throw new CustomException(ResultCodeEnum.PASSWORD_TOO_WEAK);
        }
    }

    private static boolean containsUpperCase(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isUpperCase(c)) return true;
        }
        return false;
    }

    private static boolean containsLowerCase(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isLowerCase(c)) return true;
        }
        return false;
    }

    private static boolean containsDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private static boolean containsSpecialChar(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) return true;
        }
        return false;
    }
}
