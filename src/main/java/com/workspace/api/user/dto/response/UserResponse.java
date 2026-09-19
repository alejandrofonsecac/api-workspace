package com.workspace.api.user.dto.response;

import com.workspace.api.user.domain.User;
import com.workspace.api.util.domain.Role;

public record UserResponse(
        String id,
        String name,
        String email,
        Role role) {

    public UserResponse(User user) {
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }
}
