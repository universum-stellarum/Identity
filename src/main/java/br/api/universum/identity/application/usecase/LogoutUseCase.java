package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.RefreshTokenRepository;
import br.api.universum.identity.application.port.TokenHasher;
import br.api.universum.identity.application.port.UnitOfWork;

/**
 * Revokes the given refresh token. Idempotent: an unknown or already revoked token is ignored.
 * The access token is not revoked; it expires on its own.
 */
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;
    private final UnitOfWork unitOfWork;

    public LogoutUseCase(RefreshTokenRepository refreshTokens, TokenHasher tokenHasher, UnitOfWork unitOfWork) {
        this.refreshTokens = refreshTokens;
        this.tokenHasher = tokenHasher;
        this.unitOfWork = unitOfWork;
    }

    public void execute(String rawRefreshToken) {
        unitOfWork.run(() -> refreshTokens.findByTokenHashForUpdate(tokenHasher.hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokens.save(token);
                }));
    }
}
