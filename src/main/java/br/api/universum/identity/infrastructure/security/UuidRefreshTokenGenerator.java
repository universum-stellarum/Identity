package br.api.universum.identity.infrastructure.security;

import br.api.universum.identity.application.port.RefreshTokenGeneratorPort;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidRefreshTokenGenerator implements RefreshTokenGeneratorPort {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
