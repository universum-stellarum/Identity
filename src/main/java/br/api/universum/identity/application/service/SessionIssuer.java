package br.api.universum.identity.application.service;

import br.api.universum.identity.application.dto.AuthenticationResult;
import br.api.universum.identity.application.port.AccessTokenIssuer;
import br.api.universum.identity.application.port.AccessTokenIssuer.AccessToken;
import br.api.universum.identity.application.port.RefreshTokenGenerator;
import br.api.universum.identity.application.port.RefreshTokenRepository;
import br.api.universum.identity.application.port.TokenHasher;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.domain.model.User;

import java.time.Clock;
import java.time.Duration;

/**
 * Opens a session for an authenticated user: issues the access token and stores a new refresh token.
 * Shared by login and refresh; must run inside the caller's unit of work.
 */
public class SessionIssuer {

    public static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);

    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenHasher tokenHasher;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public SessionIssuer(AccessTokenIssuer accessTokens,
                         RefreshTokenGenerator refreshTokenGenerator,
                         TokenHasher tokenHasher,
                         RefreshTokenRepository refreshTokens,
                         Clock clock) {
        this.accessTokens = accessTokens;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.tokenHasher = tokenHasher;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

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
