package com.workspace.api.user.security;

import com.workspace.api.user.security.filter.SessionRequestFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.assertThat;

class SessionRequestFilterTest {
    @Test
    void productionRequiresHttpsAndSetsSecureHostOnlyCookie() throws Exception {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("production", "local");
        var cookie = new RefreshCookie(environment);
        var filter = new SessionRequestFilter(cookie);
        var request = new MockHttpServletRequest("POST", "/users/refresh");
        request.addHeader("X-PlannerFy-Session", "1");
        var insecure = new MockHttpServletResponse();
        var reached = new AtomicBoolean(false);
        filter.doFilter(request, insecure, (req, res) -> reached.set(true));
        assertThat(insecure.getStatus()).isEqualTo(426);
        assertThat(reached).isFalse();
        request.setSecure(true);
        var secure = new MockHttpServletResponse();
        filter.doFilter(request, secure, (req, res) -> reached.set(true));
        assertThat(reached).isTrue();
        cookie.write(secure, "test-only", Duration.ofDays(30));
        assertThat(secure.getHeader("Set-Cookie")).contains("Secure", "HttpOnly", "SameSite=Strict", "Path=/")
                .doesNotContain("Domain=");
        var clear = new MockHttpServletResponse();
        cookie.clear(clear);
        assertThat(clear.getHeader("Set-Cookie")).contains("Max-Age=0", "Secure", "HttpOnly", "Path=/");
    }

    @Test
    void defaultProfileIsSecureAndOnlyExplicitLocalOrTestAllowsHttp() {
        assertThat(new RefreshCookie(new MockEnvironment()).requiresHttps()).isTrue();
        var local = new MockEnvironment();
        local.setActiveProfiles("local");
        assertThat(new RefreshCookie(local).requiresHttps()).isFalse();
    }
}
