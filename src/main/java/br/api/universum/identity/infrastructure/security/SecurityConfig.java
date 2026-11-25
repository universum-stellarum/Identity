package br.api.universum.identity.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança HTTP do identity. Login, refresh, logout, JWKS e health são públicos; as demais rotas
 * exigem um access token válido, conferido com a própria chave do serviço.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_AUTHENTICATION_POSTS = {
            "/identity/api/v1/auth/login",
            "/identity/api/v1/auth/refresh",
            "/identity/api/v1/auth/logout"
    };

    private static final String[] PUBLIC_GETS = {
            "/.well-known/jwks.json",
            "/health",
            "/identity/amiworking",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * As rotas públicas ficam numa cadeia própria, sem o resource server de JWT: um header
     * Authorization vencido não pode transformá-las em 401 (login, refresh e logout existem
     * justamente para recuperar dessa situação).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) {
        statelessApi(http)
                .securityMatchers(matchers -> matchers
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTHENTICATION_POSTS)
                        .requestMatchers(HttpMethod.GET, PUBLIC_GETS))
                .authorizeHttpRequests(
                        authorize -> authorize.anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * Todo o resto exige {@code Authorization: Bearer <access token>}, validado pelo
     * {@code JwtDecoder} do {@link br.api.universum.identity.infrastructure.security.jwt.JwtConfig}.
     * O actuator, fora o health, fica fechado.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        statelessApi(http)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").denyAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** O CORS fica no gateway, que é o único ponto de entrada. */
    private HttpSecurity statelessApi(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
