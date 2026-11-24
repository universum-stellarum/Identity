package br.api.universum.identity.application.usecases.auth;

import br.api.universum.identity.application.abstractions.AccessTokenIssuer.AccessToken;
import br.api.universum.identity.application.abstractions.AccessTokenIssuer;
import br.api.universum.identity.application.abstractions.RefreshTokenGenerator;
import br.api.universum.identity.application.abstractions.RefreshTokenRepository;
import br.api.universum.identity.application.abstractions.TokenHasher;
import br.api.universum.identity.application.usecases.auth.response.TokenResponse;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Abre uma sessão para um usuário autenticado: emite o access token e grava um novo refresh token.
 * Compartilhado por login e refresh; roda dentro da transação de quem chama.
 */
@Component
@RequiredArgsConstructor
class SessionIssuer {

    static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);

    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenHasher tokenHasher;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    TokenResponse open(User user) {
        AccessToken accessToken = accessTokens.issue(user);
        String refreshToken = refreshTokenGenerator.generate();

        // O banco guarda só o hash; o valor entregue ao cliente nunca é persistido.
        refreshTokens.save(RefreshToken.issue(
                tokenHasher.hash(refreshToken),
                user.getId(),
                clock.instant(),
                REFRESH_TOKEN_VALIDITY
        ));

        return TokenResponse.bearer(accessToken.value(), refreshToken, accessToken.expiresInSeconds());
    }
}
