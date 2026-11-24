package br.api.universum.identity.infrastructure.config;

import br.api.universum.identity.infrastructure.security.jwt.JwtKeyProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

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
