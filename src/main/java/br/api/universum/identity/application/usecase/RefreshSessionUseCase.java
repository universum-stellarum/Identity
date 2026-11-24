package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.AuthenticationResult;
import br.api.universum.identity.application.port.RefreshTokenRepository;
import br.api.universum.identity.application.port.TokenHasher;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.application.service.SessionIssuer;
import br.api.universum.identity.domain.exception.InvalidRefreshTokenException;
import br.api.universum.identity.domain.exception.UserBlockedException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

/**
 * Exchanges a valid refresh token for a new token pair, revoking the token used (rotation).
 * Presenting a token that was already rotated revokes every session of the user.
 */
@Slf4j
@RequiredArgsConstructor
public class RefreshSessionUseCase {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;
    private final SessionIssuer sessions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    // A revogação de todas as sessões em caso de reuso precisa ser persistida, mesmo com a
    // InvalidRefreshTokenException lançada em seguida: a unit of work commita em DomainException.
    public AuthenticationResult execute(String rawRefreshToken) {
        return unitOfWork.execute(() -> {
            RefreshToken current = refreshTokens.findByTokenHashForUpdate(tokenHasher.hash(rawRefreshToken))
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
        });
    }
}
