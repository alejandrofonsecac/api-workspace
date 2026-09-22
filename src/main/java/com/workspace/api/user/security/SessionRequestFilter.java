package com.workspace.api.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

/** Custom header + exact-origin credentialed CORS prevents browser CSRF, including login CSRF. */
public class SessionRequestFilter extends OncePerRequestFilter {
    private static final Set<String> ENDPOINTS = Set.of(
            "/users/login", "/users/register", "/users/refresh", "/users/logout", "/users/logout-all");
    private final RefreshCookie cookie;

    public SessionRequestFilter(RefreshCookie cookie) { this.cookie = cookie; }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        if (cookie.requiresHttps() && !request.isSecure()) {
            reject(response, 426, "HTTPS_REQUIRED");
            return;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if ("POST".equals(request.getMethod()) && ENDPOINTS.contains(path)
                && !"1".equals(request.getHeader("X-PlannerFy-Session"))) {
            reject(response, 403, "SESSION_HEADER_REQUIRED");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"status\":" + status + ",\"code\":\"" + code
                + "\",\"message\":\"Session request rejected\",\"details\":[]}");
    }
}
