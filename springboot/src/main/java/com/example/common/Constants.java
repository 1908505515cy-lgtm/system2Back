package com.example.common;

import java.security.SecureRandom;
import java.util.stream.Collectors;

public interface Constants {
    String TOKEN = "token";

    /**
     * 生成随机临时密码（用于管理员重置密码）。
     * 生成 12 位随机字母数字字符串。
     */
    static String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        return random.ints(12, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());
    }

}
