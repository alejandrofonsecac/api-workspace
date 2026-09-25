package com.workspace.api.user.security;

import com.workspace.api.user.security.exception.AuthenticationRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {
    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAuthenticationHasSpecificUnauthorizedException() {
        assertThatThrownBy(currentUser::id)
                .isInstanceOf(AuthenticationRequiredException.class)
                .satisfies(exception -> assertThat(((AuthenticationRequiredException) exception)
                        .getStatusCode().value()).isEqualTo(401));
    }

    @Test
    void anonymousAuthenticationIsRejected() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("test-key", "anonymousUser", List.of(() -> "ROLE_ANONYMOUS")));

        assertThatThrownBy(currentUser::id).isInstanceOf(AuthenticationRequiredException.class);
    }

    @Test
    void authenticatedUserIdComesFromPrincipalName() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-123", null, List.of()));

        assertThat(currentUser.id()).isEqualTo("user-123");
    }
}
