package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.AuthenticationResult;
import br.api.universum.identity.application.dto.LoginCommand;
import br.api.universum.identity.application.port.PasswordHasherPort;
import br.api.universum.identity.application.port.RefreshTokenRepositoryPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.application.port.UserRepositoryPort;
import br.api.universum.identity.application.service.SessionIssuer;
import br.api.universum.identity.domain.exception.InvalidCredentialsException;
import br.api.universum.identity.domain.exception.UserBlockedException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

/**
 * Authenticates email and password and opens a new session. A new login revokes the user's
 * previous refresh tokens (single session). Too many wrong passwords lock the account.
 */
@Slf4j
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepositoryPort users;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final PasswordHasherPort passwordHasher;
    private final SessionIssuer sessions;
    private final UnitOfWorkPort unitOfWork;
    private final Clock clock;

    // A failed attempt is a DomainException: the unit of work still commits the attempt counter.
    public AuthenticationResult execute(LoginCommand command) {
        return unitOfWork.execute(() -> {
            User user = verifyCredentials(command.email(), command.password());
            refreshTokens.revokeAllByUserId(user.getId());
            return sessions.open(user);
        });
    }

    private User verifyCredentials(String email, String rawPassword) {
        log.info("[LOGIN] Login attempt started for email: '{}'", email);

        User user = users.findByEmail(email).orElseThrow(() -> {
            log.warn("[LOGIN] Login failed: email '{}' is not registered in the system", email);
            return new InvalidCredentialsException();
        });

        Instant now = clock.instant();

        if (user.isLocked(now)) {
            log.warn("[LOGIN] Login failed: account '{}' is temporarily locked until {}",
                    email, user.getLockExpiredAt());
            throw new UserBlockedException();
        }

        if (passwordHasher.matches(rawPassword, user.getPassword())) {
            log.info("[LOGIN] Login succeeded for user: '{}' (ID: {})", email, user.getId());
            user.registerSuccessfulLogin();
            return users.save(user);
        }

        user.registerFailedLogin(now);
        users.save(user);

        if (user.isLocked(now)) {
            log.error("[LOGIN] Login failed: incorrect password for email '{}'. "
                    + "Attempt limit reached! Account temporarily LOCKED until {}", email, user.getLockExpiredAt());
            throw new UserBlockedException();
        }

        log.warn("[LOGIN] Login failed: incorrect password for email '{}'. Consecutive failed attempts: {}/{}",
                email, user.getFailedAttempts(), User.MAX_FAILED_ATTEMPTS);
        throw new InvalidCredentialsException();
    }
}
