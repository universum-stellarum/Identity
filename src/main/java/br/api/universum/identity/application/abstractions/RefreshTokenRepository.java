package br.api.universum.identity.application.abstractions;

import br.api.universum.identity.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

/** Armazena os refresh tokens emitidos. Os tokens são localizados sempre pelo hash, nunca pelo valor. */
public interface RefreshTokenRepository {

    /**
     * Busca o token e o bloqueia até o fim da unidade de trabalho, para que requisições
     * concorrentes não consumam o mesmo refresh token duas vezes.
     */
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    /** Insere um token novo (ainda sem id) ou atualiza um existente. */
    <S extends RefreshToken> S save(S token);

    /** Revoga todos os refresh tokens do usuário, encerrando todas as sessões dele. */
    void revokeAllByUserId(UUID userId);
}
