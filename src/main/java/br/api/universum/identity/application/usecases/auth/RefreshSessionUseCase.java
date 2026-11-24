package br.api.universum.identity.application.usecases.auth;

import br.api.universum.identity.application.abstractions.RefreshTokenRepository;
import br.api.universum.identity.application.abstractions.TokenHasher;
import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.application.usecases.auth.request.RefreshTokenRequest;
import br.api.universum.identity.application.usecases.auth.response.TokenResponse;
import br.api.universum.identity.domain.exception.BusinessException;
import br.api.universum.identity.domain.exception.InvalidRefreshTokenException;
import br.api.universum.identity.domain.exception.UserBlockedException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Troca um refresh token válido por um novo par de tokens e revoga o token usado (rotação).
 * Apresentar um token que já foi rotacionado revoga todas as sessões do usuário.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshSessionUseCase {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;
    private final SessionIssuer sessions;
    private final Clock clock;

    // noRollbackFor: a revogação de todas as sessões em caso de reuso precisa ser persistida,
    // mesmo com a InvalidRefreshTokenException lançada em seguida.
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse execute(RefreshTokenRequest request) {
        RefreshToken current = refreshTokens.findByTokenHashForUpdate(tokenHasher.hash(request.refreshToken()))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            // Um refresh token já rotacionado voltou a ser apresentado: provável roubo.
            // Encerra todas as sessões do usuário, e o cliente precisa fazer login de novo.
            log.warn("[REFRESH] Reuso de refresh token revogado. Sessões do usuário {} encerradas",
                    current.getUserId());
            refreshTokens.revokeAllByUserId(current.getUserId());
            throw new InvalidRefreshTokenException();
        }

        Instant now = clock.instant();

        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = users.findById(current.getUserId()).orElseThrow(UserNotFoundException::new);

        current.revoke();
        refreshTokens.save(current);

        if (user.isLocked(now)) {
            throw new UserBlockedException();
        }

        return sessions.open(user);
    }
}
