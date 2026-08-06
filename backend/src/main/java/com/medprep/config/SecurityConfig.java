package com.medprep.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/specialties/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
                )
                .build();
    }

    /**
     * Convierte el JWT de Supabase en una autenticación cuyo <b>principal es el
     * UUID del usuario</b> (claim {@code sub}), de modo que los controllers
     * puedan usar {@code @AuthenticationPrincipal String userId} directamente.
     * El rol se extrae de {@code app_metadata.role} (o {@code user_metadata.role}).
     */
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter() {
        return jwt -> {
            String role = "student";
            try {
                Map<String, Object> appMeta = jwt.getClaimAsMap("app_metadata");
                if (appMeta != null && appMeta.get("role") != null) {
                    role = appMeta.get("role").toString();
                } else {
                    Map<String, Object> userMeta = jwt.getClaimAsMap("user_metadata");
                    if (userMeta != null && userMeta.get("role") != null) {
                        role = userMeta.get("role").toString();
                    }
                }
            } catch (Exception ignored) {
                // rol por defecto: student
            }

            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
            );

            // principal = sub (UUID); credentials = jwt (por si se necesita el token)
            return new UsernamePasswordAuthenticationToken(jwt.getSubject(), jwt, authorities);
        };
    }
}
