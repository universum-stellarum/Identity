package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.RefreshTokenRepositoryPort;
import br.api.universum.identity.application.port.TokenHasherPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;

import lombok.RequiredArgsConstructor;

/**
 * Revokes the given refresh token. Idempotent: an unknown or already revoked token is ignored.
 * The access token is not revoked; it expires on its own.
 */
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokens;
    private final TokenHasherPort tokenHasher;
    private final UnitOfWorkPort unitOfWork;

    public void execute(String rawRefreshToken) {
        unitOfWork.run(() -> refreshTokens.findByTokenHashForUpdate(tokenHasher.hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokens.save(token);
                }));
    }
}
