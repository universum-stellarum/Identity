package br.api.universum.identity.infrastructure.security;

import br.api.universum.identity.application.port.RefreshTokenGenerator;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidRefreshTokenGenerator implements RefreshTokenGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
