package br.api.universum.identity.application.usecases.keys.response;

import br.api.universum.identity.domain.model.SigningKey;

import java.time.Instant;
import java.util.UUID;

/** Metadados de uma chave de assinatura. Nunca carrega o material da chave. */
public record SigningKeyResponse(
        UUID keyId,
        String algorithm,
        Instant createdAt) {

    public static SigningKeyResponse from(SigningKey key) {
        return new SigningKeyResponse(key.getKeyId(), SigningKey.ALGORITHM, key.getRotatedAt());
    }
}
