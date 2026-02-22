package com.creatoros.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class TokenHashUtil {

    private TokenHashUtil() {
    }

    public static String sha256Hex(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        byte[] digest = sha256(rawToken.getBytes(StandardCharsets.UTF_8));
        return toHex(digest);
    }

    public static boolean constantTimeEqualsHex(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.US_ASCII);
        byte[] bBytes = b.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
