package com.workspace.api.support;

import com.workspace.api.user.domain.User;
import com.workspace.api.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import java.util.List;

public abstract class AuthenticatedTest {
    @Autowired UserRepository testUsers;
    protected User testUser;

    @BeforeEach
    void authenticate() {
        testUser = new User();
        testUser.setName("Test user");
        testUser.setEmail(testUser.getId() + "@example.com");
        testUser.setPasswordHash("unused-in-mock-authentication");
        testUsers.saveAndFlush(testUser);
        TestSecurityContextHolder.setAuthentication(new UsernamePasswordAuthenticationToken(
                testUser.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void clearAuthentication() {
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();
    }
}
