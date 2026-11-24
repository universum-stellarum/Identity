package br.api.universum.identity.application.usecases.auth;

import br.api.universum.identity.application.abstractions.RefreshTokenRepository;
import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.domain.exception.BusinessException;
import br.api.universum.identity.domain.exception.InvalidCredentialsException;
import br.api.universum.identity.domain.exception.UserBlockedException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Autentica e-mail e senha e abre uma nova sessão. Um novo login revoga os refresh tokens anteriores
 * do usuário (sessão única). Senhas erradas demais bloqueiam a conta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final SessionIssuer sessions;
    private final Clock clock;

    // noRollbackFor: uma tentativa falha precisa manter o contador de tentativas,
    // mesmo com a exceção lançada em seguida.
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse execute(LoginRequest request) {
        User user = verifyCredentials(request.email(), request.password());
        refreshTokens.revokeAllByUserId(user.getId());
        return sessions.open(user);
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

        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
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
