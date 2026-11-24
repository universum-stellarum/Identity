package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.ImmediateUnitOfWork;
import br.api.universum.identity.application.dto.AuthenticationResult;
import br.api.universum.identity.application.dto.LoginCommand;
import br.api.universum.identity.application.port.AccessTokenIssuer;
import br.api.universum.identity.application.port.AccessTokenIssuer.AccessToken;
import br.api.universum.identity.application.port.PasswordHasher;
import br.api.universum.identity.application.port.RefreshTokenGenerator;
import br.api.universum.identity.application.port.RefreshTokenRepository;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.application.service.SessionIssuer;
import br.api.universum.identity.domain.exception.InvalidCredentialsException;
import br.api.universum.identity.domain.exception.UserBlockedException;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.domain.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "john@universum.api.br";

    @Mock private UserRepository users;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private PasswordHasher passwordHasher;
    @Mock private AccessTokenIssuer accessTokens;
    @Mock private RefreshTokenGenerator refreshTokenGenerator;

    private LoginUseCase login;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        // Hash previsível para os testes: "hash:" + valor bruto.
        SessionIssuer sessions = new SessionIssuer(accessTokens, refreshTokenGenerator,
                raw -> "hash:" + raw, refreshTokens, clock);
        login = new LoginUseCase(users, refreshTokens, passwordHasher, sessions, new ImmediateUnitOfWork(), clock);

        user = User.restore(USER_ID, EMAIL, "encoded", "John", User.DEFAULT_ROLE, 0, null);
        lenient().when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubTokenGeneration() {
        when(accessTokens.issue(user)).thenReturn(new AccessToken("access-token", 900L));
        when(refreshTokenGenerator.generate()).thenReturn("refresh-token");
    }

    @Test
    void shouldRevokeOldTokensAndIssueNewPair() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret", "encoded")).thenReturn(true);
        stubTokenGeneration();

        AuthenticationResult result = login.execute(new LoginCommand(EMAIL, "secret"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.expiresIn()).isEqualTo(900L);

        verify(refreshTokens).revokeAllByUserId(USER_ID);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(captor.capture());
        // O banco guarda o hash, nunca o valor entregue ao cliente.
        assertThat(captor.getValue().getTokenHash()).isEqualTo("hash:refresh-token");
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getExpiryDate()).isEqualTo(NOW.plus(SessionIssuer.REFRESH_TOKEN_VALIDITY));
    }

    @Test
    void shouldResetPreviousFailedAttemptsAndPersistThem() {
        user.registerFailedLogin(NOW);
        user.registerFailedLogin(NOW);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret", "encoded")).thenReturn(true);
        stubTokenGeneration();

        login.execute(new LoginCommand(EMAIL, "secret"));

        assertThat(user.getFailedAttempts()).isZero();
        verify(users).save(user);
    }

    @Test
    void shouldFailForUnknownEmail() {
        when(users.findByEmail("ghost@universum.api.br")).thenReturn(Optional.empty());
        LoginCommand command = new LoginCommand("ghost@universum.api.br", "secret");

        assertThatThrownBy(() -> login.execute(command))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials.");

        verify(refreshTokens, never()).save(any());
    }

    @Test
    void shouldFailAndPersistTheAttemptWhenPasswordIsWrong() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "encoded")).thenReturn(false);
        LoginCommand command = new LoginCommand(EMAIL, "wrong");

        assertThatThrownBy(() -> login.execute(command))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials.");

        assertThat(user.getFailedAttempts()).isEqualTo(1);
        verify(users).save(user);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void shouldLockAccountOnFifthFailedAttempt() {
        for (int i = 0; i < User.MAX_FAILED_ATTEMPTS - 1; i++) {
            user.registerFailedLogin(NOW);
        }
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "encoded")).thenReturn(false);
        LoginCommand command = new LoginCommand(EMAIL, "wrong");

        assertThatThrownBy(() -> login.execute(command))
                .isInstanceOf(UserBlockedException.class)
                .hasMessageContaining("locked");

        assertThat(user.isLocked(NOW)).isTrue();
        verify(users).save(user);
    }

    @Test
    void shouldRejectLockedAccountWithoutCheckingPassword() {
        User locked = User.restore(USER_ID, EMAIL, "encoded", "John", User.DEFAULT_ROLE,
                User.MAX_FAILED_ATTEMPTS, NOW.plus(Duration.ofMinutes(10)));
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(locked));
        LoginCommand command = new LoginCommand(EMAIL, "secret");

        assertThatThrownBy(() -> login.execute(command))
                .isInstanceOf(UserBlockedException.class)
                .hasMessageContaining("locked");

        verify(passwordHasher, never()).matches(any(), any());
        verify(refreshTokens, never()).save(any());
    }
}
