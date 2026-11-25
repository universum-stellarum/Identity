package br.api.universum.identity.infrastructure.security;

import br.api.universum.identity.application.abstractions.RefreshTokenGenerator;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** Gera o refresh token como um UUID aleatório (versão 4, 122 bits aleatórios). */
@Component
public class UuidRefreshTokenGenerator implements RefreshTokenGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
