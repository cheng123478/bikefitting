package com.starrysky.aries.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 邀请码生成与解析工具 (基于 Base62)
 *
 * @author adi
 */
@Slf4j
public class InviteCodeUtil {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final long SALT = 1000000L; // 盐值，防止id=1这种编码太短且容易猜到

    /**
     * 将用户ID编码为邀请码
     *
     * @param userId 用户ID
     * @return 邀请码短链
     */
    public static String encode(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        long num = userId + SALT;
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(ALPHABET.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * 将邀请码解码为用户ID
     *
     * @param code 邀请码
     * @return 用户ID，解码失败返回null
     */
    public static Long decode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        try {
            long num = 0;
            for (int i = 0; i < code.length(); i++) {
                num = num * BASE + ALPHABET.indexOf(code.charAt(i));
            }
            long userId = num - SALT;
            return userId > 0 ? userId : null;
        } catch (Exception e) {
            log.warn("无效的邀请码解析: {}", code);
            return null;
        }
    }
}
