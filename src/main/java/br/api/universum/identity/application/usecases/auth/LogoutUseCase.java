package br.api.universum.identity.application.usecases.auth;

import br.api.universum.identity.application.abstractions.RefreshTokenRepository;
import br.api.universum.identity.application.abstractions.TokenHasher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revoga o refresh token informado. Idempotente: um token desconhecido ou já revogado é ignorado.
 * O access token não é revogado; ele expira sozinho.
 */
@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;

    @Transactional
    public void execute(RefreshTokenRequest request) {
        refreshTokens.findByTokenHashForUpdate(tokenHasher.hash(request.refreshToken()))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokens.save(token);
                });
    }
}
