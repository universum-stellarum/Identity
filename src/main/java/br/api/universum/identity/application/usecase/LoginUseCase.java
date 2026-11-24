package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.AuthenticationResult;
import br.api.universum.identity.application.dto.LoginCommand;
import br.api.universum.identity.application.port.PasswordHasher;
import br.api.universum.identity.application.port.RefreshTokenRepository;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.application.service.SessionIssuer;
import br.api.universum.identity.domain.exception.InvalidCredentialsException;
import br.api.universum.identity.domain.exception.UserBlockedException;
import br.api.universum.identity.domain.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

/**
 * Authenticates email and password and opens a new session. A new login revokes the user's
 * previous refresh tokens (single session). Too many wrong passwords lock the account.
 */
public class LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginUseCase.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public LoginUseCase(UserRepository users,
                        RefreshTokenRepository refreshTokens,
                        PasswordHasher passwordHasher,
                        SessionIssuer sessions,
                        UnitOfWork unitOfWork,
                        Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

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
