package com.workspace.api.user.security.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Cria os componentes que assinam e verificam os “tokens” JWT da API.
 *
 * O encoder assina o token no login; o decoder verifica a assinatura nas
 * próximas requisições. Ambos usam a mesma chave porque adotamos HS256.
 * JWT assinado não é criptografado: o seu conteúdo pode ser lido pelo cliente.
 *
 * Veja .docs/authentication/JWT_CONFIGURATION.md para o fluxo completo.
 */
@Configuration
public class JwtConfiguration {
    /**
     * Converte a representação Base64 do .env nos bytes da chave HMAC.
     * Base64 é apenas um formato de armazenamento, não uma proteção do segredo.
     */
    @Bean
    SecretKey jwtKey(@Value("${app.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado. Defina uma chave aleatória em Base64 no .env ou no ambiente. "
                            + "Veja .docs/authentication/JWT_CONFIGURATION.md.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET deve estar em Base64. Veja .docs/authentication/JWT_CONFIGURATION.md.", exception);
        }

        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET deve conter pelo menos 32 bytes aleatórios antes do Base64.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * O Spring injeta a chave criada acima. Nimbus é a biblioteca que implementa
     * a assinatura; ImmutableSecret entrega a chave a ela sem gerar outra chave.
     */
    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    /**
     * Aceita somente HS256 e valida assinatura, validade temporal e emissor.
     * SecurityConfiguration usa este decoder ao receber Authorization: Bearer.
     */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey, @Value("${app.jwt.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
