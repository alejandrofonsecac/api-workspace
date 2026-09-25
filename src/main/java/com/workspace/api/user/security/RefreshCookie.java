package com.workspace.api.user.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class RefreshCookie {
    public static final String NAME = "plannerfy_refresh";
    private final boolean secure;

    public RefreshCookie(Environment environment) {
        secure = environment.acceptsProfiles(Profiles.of("prod", "production"))
                || !environment.acceptsProfiles(Profiles.of("local", "test"));
    }

    public boolean requiresHttps() {
        return secure;
    }

    public void write(HttpServletResponse response, String value, Duration lifetime) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, value)
                .httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(lifetime).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    public void clear(HttpServletResponse response) {
        write(response, "", Duration.ZERO);
    }
}