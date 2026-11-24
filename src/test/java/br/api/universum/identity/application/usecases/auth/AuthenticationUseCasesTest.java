package br.api.universum.identity.application.usecases.auth;

import br.api.universum.identity.application.abstractions.AccessTokenIssuer;
import br.api.universum.identity.application.abstractions.AccessTokenIssuer.AccessToken;
import br.api.universum.identity.application.abstractions.RefreshTokenGenerator;
import br.api.universum.identity.application.abstractions.RefreshTokenRepository;
import br.api.universum.identity.application.abstractions.TokenHasher;
import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.domain.exception.InvalidCredentialsException;
import br.api.universum.identity.domain.exception.InvalidRefreshTokenException;
import br.api.universum.identity.domain.exception.UserBlockedException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.domain.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "john@universum.api.br";

    // Hash previsível para os testes: "hash:" + valor bruto.
    private static final TokenHasher HASHER = raw -> "hash:" + raw;

    @Mock private UserRepository users;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccessTokenIssuer accessTokens;
    @Mock private RefreshTokenGenerator refreshTokenGenerator;

    private LoginUseCase login;
    private RefreshSessionUseCase refresh;
    private LogoutUseCase logout;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SessionIssuer sessions = new SessionIssuer(accessTokens, refreshTokenGenerator, HASHER, refreshTokens, clock);
        login = new LoginUseCase(users, refreshTokens, passwordEncoder, sessions, clock);
        refresh = new RefreshSessionUseCase(users, refreshTokens, HASHER, sessions, clock);
        logout = new LogoutUseCase(refreshTokens, HASHER);

        user = User.restore(USER_ID, EMAIL, "encoded", "John", User.DEFAULT_ROLE, 0, null);
        lenient().when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(refreshTokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubTokenGeneration() {
        when(accessTokens.issue(user)).thenReturn(new AccessToken("access-token", 900L));
        when(refreshTokenGenerator.generate()).thenReturn("refresh-token");
    }

    private static RefreshToken stored(String raw, Instant issuedAt) {
        return RefreshToken.restore(UUID.randomUUID(), HASHER.hash(raw), USER_ID,
                issuedAt.plus(Duration.ofDays(7)), false);
    }

    @Nested
    class Login {

        @Test
        void shouldRevokeOldTokensAndIssueNewPair() {
            when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
            stubTokenGeneration();

            TokenResponse response = login.execute(new LoginRequest(EMAIL, "secret"));

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900L);

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
            when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
            stubTokenGeneration();

            login.execute(new LoginRequest(EMAIL, "secret"));

            assertThat(user.getFailedAttempts()).isZero();
            verify(users).save(user);
        }

        @Test
        void shouldFailForUnknownEmail() {
            when(users.findByEmail("ghost@universum.api.br")).thenReturn(Optional.empty());
            LoginRequest request = new LoginRequest("ghost@universum.api.br", "secret");

            assertThatThrownBy(() -> login.execute(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid credentials.");

            verify(refreshTokens, never()).save(any());
        }

        @Test
        void shouldFailAndPersistTheAttemptWhenPasswordIsWrong() {
            when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
            LoginRequest request = new LoginRequest(EMAIL, "wrong");

            assertThatThrownBy(() -> login.execute(request))
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
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
            LoginRequest request = new LoginRequest(EMAIL, "wrong");

            assertThatThrownBy(() -> login.execute(request))
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
            LoginRequest request = new LoginRequest(EMAIL, "secret");

            assertThatThrownBy(() -> login.execute(request))
                    .isInstanceOf(UserBlockedException.class)
                    .hasMessageContaining("locked");

            verify(passwordEncoder, never()).matches(any(), any());
            verify(refreshTokens, never()).save(any());
        }
    }

    @Nested
    class Refresh {

        @Test
        void shouldRotateTokens() {
            RefreshToken oldToken = stored("old-token", NOW);
            when(refreshTokens.findByTokenHashForUpdate(HASHER.hash("old-token"))).thenReturn(Optional.of(oldToken));
            when(users.findById(USER_ID)).thenReturn(Optional.of(user));
            stubTokenGeneration();

            TokenResponse response = refresh.execute(new RefreshTokenRequest("old-token"));

            assertThat(oldToken.isRevoked()).isTrue();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokens, times(2)).save(captor.capture());
            List<RefreshToken> saved = captor.getAllValues();
            assertThat(saved.get(0)).isSameAs(oldToken);
            assertThat(saved.get(1).getTokenHash()).isEqualTo(HASHER.hash("refresh-token"));
            assertThat(saved.get(1).getUserId()).isEqualTo(USER_ID);
            assertThat(saved.get(1).isRevoked()).isFalse();
        }

        @Test
        void shouldRejectLockedUserButStillConsumeTheToken() {
            RefreshToken token = stored("locked-token", NOW);
            User locked = User.restore(USER_ID, EMAIL, "encoded", "John", User.DEFAULT_ROLE,
                    User.MAX_FAILED_ATTEMPTS, NOW.plusSeconds(60));
            when(refreshTokens.findByTokenHashForUpdate(HASHER.hash("locked-token"))).thenReturn(Optional.of(token));
            when(users.findById(USER_ID)).thenReturn(Optional.of(locked));

            assertThatThrownBy(() -> refresh.execute(new RefreshTokenRequest("locked-token")))
                    .isInstanceOf(UserBlockedException.class);

            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokens).save(token);
            verify(accessTokens, never()).issue(any());
        }

        @Test
        void shouldFailWhenTokenDoesNotExist() {
            when(refreshTokens.findByTokenHashForUpdate(HASHER.hash("unknown"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refresh.execute(new RefreshTokenRequest("unknown")))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessage("Refresh token is invalid, revoked, or expired.");
        }

        @Test
        void shouldRevokeAllSessionsWhenRevokedTokenIsReused() {
            RefreshToken revoked = stored("reused-token", NOW);
            revoked.revoke();
            when(refreshTokens.findByTokenHashForUpdate(HASHER.hash("reused-token"))).thenReturn(Optional.of(revoked));

            assertThatThrownBy(() -> refresh.execute(new RefreshTokenRequest("reused-token")))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessage("Refresh token is invalid, revoked, or expired.");

            // Reuso de token já rotacionado: todas as sessões do usuário são encerradas.
            verify(refreshTokens).revokeAllByUserId(USER_ID);
            verify(refreshTokens, never()).save(any());
        }

        @Test
        void shouldFailWhenTokenIsExpired() {
            RefreshToken expired = stored("expired-token", NOW.minus(Duration.ofDays(10)));
            when(refreshTokens.findByTokenHashForUpdate(HASHER.hash("expired-token"))).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> refresh.execute(new RefreshTokenRequest("expired-token")))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessage("Refresh token is invalid, revoked, or expired.");

            verify(refreshTokens, never()).save(any());
        }

        @Test
        void shouldFailWhenUserNoLongerExists() {
            RefreshToken token = stored("orphan-token", NOW);
            when(refreshTokens.findByTokenHashForUpdate(HASHER.hash("orphan-token"))).thenReturn(Optional.of(token));
            when(users.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refresh.execute(new RefreshTokenRequest("orphan-token")))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found.");

            assertThat(token.isRevoked()).isFalse();
        }
    }

    @Nested
    class Logout {

        @Test
        void shouldRevokeTheGivenToken() {
            RefreshToken token = stored("token", NOW);
            when(refreshTokens.findByTokenHashForUpdate(HASHER.hash("token"))).thenReturn(Optional.of(token));

            logout.execute(new RefreshTokenRequest("token"));

            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokens).save(token);
        }

        @Test
        void shouldIgnoreUnknownTokens() {
            when(refreshTokens.findByTokenHashForUpdate(HASHER.hash("unknown"))).thenReturn(Optional.empty());

            assertThatCode(() -> logout.execute(new RefreshTokenRequest("unknown"))).doesNotThrowAnyException();
            verify(refreshTokens, never()).save(any());
        }
    }
}
