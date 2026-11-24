package br.api.universum.identity.application.port;

import br.api.universum.identity.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    /**
     * Busca o token e o bloqueia até o fim da unidade de trabalho, para que requisições
     * concorrentes não consumam o mesmo refresh token duas vezes.
     */
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    /** Insere um token novo (ainda sem id) ou atualiza um existente. */
    <S extends RefreshToken> S save(S token);

    void revokeAllByUserId(UUID userId);
}
