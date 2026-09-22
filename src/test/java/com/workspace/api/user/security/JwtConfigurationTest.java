package com.workspace.api.user.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                var placeholders = new PropertySourcesPlaceholderConfigurer();
                placeholders.setEnvironment(context.getEnvironment());
                context.addBeanFactoryPostProcessor(placeholders);
            })
            .withUserConfiguration(JwtConfiguration.class)
            .withPropertyValues("app.jwt.issuer=jwt-configuration-test");

    @Test
    void missingSecretExplainsHowToFixStartup() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "JWT_SECRET não configurado. Defina uma chave aleatória em Base64 no .env ou no ambiente. "
                            + "Veja .docs/authentication/JWT_CONFIGURATION.md.");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not%base64", "c2hvcnQ="})
    void rejectsEmptyMalformedAndShortSecrets(String secret) {
        runner.withPropertyValues("app.jwt.secret=" + secret).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("JWT_SECRET");
        });
    }

    @Test
    void configuredEncoderAndDecoderUseTheSameKey() {
        runner.withPropertyValues(
                "app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1tdXN0LW5ldmVyLWJlLXVzZWQtaW4tcHJvZA==")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    JwtEncoder encoder = context.getBean(JwtEncoder.class);
                    JwtDecoder decoder = context.getBean(JwtDecoder.class);
                    var claims = JwtClaimsSet.builder().issuer("jwt-configuration-test").subject("user-id")
                            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
                    String token = encoder.encode(JwtEncoderParameters.from(
                            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
                    assertThat(decoder.decode(token).getSubject()).isEqualTo("user-id");
                });
    }
}
