package br.api.universum.identity.application.service;

import br.api.universum.identity.application.dto.AuthenticationResult;
import br.api.universum.identity.application.port.AccessTokenIssuerPort;
import br.api.universum.identity.application.port.AccessTokenIssuerPort.AccessToken;
import br.api.universum.identity.application.port.RefreshTokenGeneratorPort;
import br.api.universum.identity.application.port.RefreshTokenRepositoryPort;
import br.api.universum.identity.application.port.TokenHasherPort;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;

/**
 * Opens a session for an authenticated user: issues the access token and stores a new refresh token.
 * Shared by login and refresh; must run inside the caller's unit of work.
 */
@RequiredArgsConstructor
public class SessionIssuer {

    public static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);

    private final AccessTokenIssuerPort accessTokens;
    private final RefreshTokenGeneratorPort refreshTokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final Clock clock;

    public AuthenticationResult open(User user) {
        AccessToken accessToken = accessTokens.issue(user);
        String refreshToken = refreshTokenGenerator.generate();

        // O banco guarda só o hash; o valor entregue ao cliente nunca é persistido.
        refreshTokens.save(RefreshToken.issue(
                tokenHasher.hash(refreshToken),
                user.getId(),
                clock.instant(),
                REFRESH_TOKEN_VALIDITY
        ));

        return new AuthenticationResult(accessToken.value(), refreshToken, accessToken.expiresInSeconds());
    }
}
