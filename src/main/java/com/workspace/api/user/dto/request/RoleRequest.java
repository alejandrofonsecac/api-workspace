package com.workspace.api.user.dto.request;

import com.workspace.api.util.domain.Role;
import jakarta.validation.constraints.NotNull;

public record RoleRequest(@NotNull Role role) {}
