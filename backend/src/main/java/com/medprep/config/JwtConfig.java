package com.medprep.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Bean
    @org.springframework.context.annotation.Primary
    public JwtDecoder jwtDecoder() {
        String jwksUri = supabaseUrl + "/auth/v1/.well-known/jwks.json";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();

        String issuer = supabaseUrl + "/auth/v1";
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer)
        ));

        return decoder;
    }
}
