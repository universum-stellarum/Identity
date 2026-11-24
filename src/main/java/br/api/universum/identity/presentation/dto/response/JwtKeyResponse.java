package br.api.universum.identity.presentation.dto.response;

import br.api.universum.identity.application.dto.SigningKeyResult;

import java.time.Instant;
import java.util.UUID;

public record JwtKeyResponse(
        UUID keyId,
        String algorithm,
        Instant createdAt) {

    public static JwtKeyResponse from(SigningKeyResult key) {
        return new JwtKeyResponse(key.keyId(), key.algorithm(), key.rotatedAt());
    }
}
