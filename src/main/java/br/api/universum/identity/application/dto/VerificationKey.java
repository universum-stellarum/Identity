package br.api.universum.identity.application.dto;

import br.api.universum.identity.domain.model.SigningKey;

import java.util.UUID;

/**
 * The public half of the active signing key, used by other services to verify access tokens.
 *
 * @param publicKey Base64 of the X.509 encoded RSA public key
 */
public record VerificationKey(UUID keyId, String algorithm, String publicKey) {

    public static VerificationKey from(SigningKey key) {
        return new VerificationKey(key.getKeyId(), SigningKey.ALGORITHM, key.getPublicKey());
    }
}
