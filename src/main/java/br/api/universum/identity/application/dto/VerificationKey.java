package br.api.universum.identity.application.dto;

import br.api.universum.identity.domain.model.SigningKey;

import java.util.UUID;

/**
 * A metade pública da chave de assinatura ativa, usada pelos outros serviços para validar os
 * access tokens.
 *
 * @param publicKey Base64 da chave pública RSA codificada em X.509
 */
public record VerificationKey(UUID keyId, String algorithm, String publicKey) {

    public static VerificationKey from(SigningKey key) {
        return new VerificationKey(key.getKeyId(), SigningKey.ALGORITHM, key.getPublicKey());
    }
}
