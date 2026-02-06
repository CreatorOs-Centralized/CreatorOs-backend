package com.creatoros.auth.dto;

import java.util.Set;

public record UserDto(
        String id,
        String username,
        String email,
        Set<String> roles
) {
}
