package br.api.universum.identity.application.dto;

import br.api.universum.identity.domain.model.SigningKey;

import java.time.Instant;
import java.util.UUID;

/** Metadata of a signing key. Never carries key material. */
public record SigningKeyResult(UUID keyId, String algorithm, Instant rotatedAt) {

    public static SigningKeyResult from(SigningKey key) {
        return new SigningKeyResult(key.getKeyId(), SigningKey.ALGORITHM, key.getRotatedAt());
    }
}
