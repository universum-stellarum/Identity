package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.RefreshTokenRepository;
import br.api.universum.identity.application.port.TokenHasher;
import br.api.universum.identity.application.port.UnitOfWork;

import lombok.RequiredArgsConstructor;

/**
 * Revokes the given refresh token. Idempotent: an unknown or already revoked token is ignored.
 * The access token is not revoked; it expires on its own.
 */
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;
    private final UnitOfWork unitOfWork;

    public void execute(String rawRefreshToken) {
        unitOfWork.run(() -> refreshTokens.findByTokenHashForUpdate(tokenHasher.hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokens.save(token);
                }));
    }
}
