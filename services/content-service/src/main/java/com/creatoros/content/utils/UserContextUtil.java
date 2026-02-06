package com.creatoros.content.utils;

import java.util.UUID;

public final class UserContextUtil {

    private UserContextUtil() {
    }

    public static UUID getCurrentUserId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }
}
