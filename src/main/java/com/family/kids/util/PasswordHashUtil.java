package com.family.kids.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 口令哈希工具：PBKDF2-HMAC-SHA256（JDK 自带 javax.crypto，无需引入 spring-security-crypto）。
 * 存储格式：PBKDF2$&lt;迭代次数&gt;$&lt;saltBase64&gt;$&lt;hashBase64&gt;
 */
public final class PasswordHashUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "PBKDF2";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHashUtil() {
    }

    /** 生成可入库的哈希串 */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = pbkdf2(rawPassword, salt, ITERATIONS, KEY_BITS);
        return PREFIX + "$" + ITERATIONS + "$" + b64(salt) + "$" + b64(key);
    }

    /** 校验明文口令是否匹配库中哈希串 */
    public static boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null || !stored.startsWith(PREFIX + "$")) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword, salt, Integer.parseInt(parts[1]), expected.length * 8);
            // 定长比较，防时序侧信道
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String raw, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, keyBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] key = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("口令哈希失败", e);
        }
    }

    private static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /** 生成 data.sql 种子密码用：java PasswordHashUtil.java 123456 */
    public static void main(String[] args) {
        System.out.println(hash(args.length > 0 ? args[0] : "123456"));
    }
}
