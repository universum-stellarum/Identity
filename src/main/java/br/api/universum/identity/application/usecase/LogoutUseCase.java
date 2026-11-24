package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.RefreshTokenRepositoryPort;
import br.api.universum.identity.application.port.TokenHasherPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;

import lombok.RequiredArgsConstructor;

/**
 * Revoga o refresh token informado. Idempotente: um token desconhecido ou já revogado é ignorado.
 * O access token não é revogado; ele expira sozinho.
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
