package com.workspace.api.user.security.config;

import com.workspace.api.user.domain.User;
import com.workspace.api.user.repository.UserRepository;
import com.workspace.api.user.security.RefreshCookie;
import com.workspace.api.user.security.filter.SessionRequestFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder, UserRepository users, RefreshCookie cookie){
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new SessionRequestFilter(cookie),
                        org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/register", "/users/login", "/users/refresh", "/users/logout").permitAll()
                        .requestMatchers("/users/*/role").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder)
                        .jwtAuthenticationConverter(token -> {
                            if (token.getSubject() == null || token.getExpiresAt() == null) {
                                throw new OAuth2AuthenticationException("invalid_token");
                            }

                            var user = users.findById(token.getSubject()).filter(User::isEnabled)
                                    .orElseThrow(() -> new OAuth2AuthenticationException("invalid_token"));

                            return new JwtAuthenticationToken(token,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())), user.getId());
                        })))
                .build();
    }
}
