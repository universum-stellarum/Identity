package br.api.universum.identity.infrastructure.security.jwt;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

/**
 * Expõe como beans o encoder (assina tokens) e o decoder (valida tokens) do Spring Security.
 * Os dois vêm do {@link JwtKeyProvider} e sempre usam a chave ativa.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtKeyProvider provider) {
        return provider.jwtEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtKeyProvider provider) {
        return provider.jwtDecoder();
    }
}
