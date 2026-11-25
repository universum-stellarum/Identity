package br.api.universum.identity.application.abstractions;

import br.api.universum.identity.domain.model.SigningKey;

import java.util.Optional;

/** Armazena a chave que assina os access tokens (registro único, veja {@link SigningKey}). */
public interface SigningKeyRepository {

    /**
     * Carrega o registro da chave, com o par já gerado ou ainda vazio. Vazio quando o próprio
     * registro não existe (migration V2 ausente).
     */
    Optional<SigningKey> find();

    <S extends SigningKey> S save(S key);
}
