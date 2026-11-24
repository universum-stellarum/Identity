package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.ImmediateUnitOfWork;
import br.api.universum.identity.application.dto.AuthenticationResult;
import br.api.universum.identity.application.port.AccessTokenIssuer;
import br.api.universum.identity.application.port.AccessTokenIssuer.AccessToken;
import br.api.universum.identity.application.port.RefreshTokenGenerator;
import br.api.universum.identity.application.port.RefreshTokenRepository;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.application.service.SessionIssuer;
import br.api.universum.identity.domain.exception.InvalidRefreshTokenException;
import br.api.universum.identity.domain.exception.UserBlockedException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshSessionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private UserRepository users;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private AccessTokenIssuer accessTokens;
    @Mock private RefreshTokenGenerator refreshTokenGenerator;

    private RefreshSessionUseCase refresh;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SessionIssuer sessions = new SessionIssuer(accessTokens, refreshTokenGenerator,
                RefreshSessionUseCaseTest::hash, refreshTokens, clock);
        refresh = new RefreshSessionUseCase(users, refreshTokens, RefreshSessionUseCaseTest::hash,
                sessions, new ImmediateUnitOfWork(), clock);

        user = User.restore(USER_ID, "john@universum.api.br", "encoded", "John", User.DEFAULT_ROLE, 0, null);
        lenient().when(refreshTokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Hash previsível para os testes: "hash:" + valor bruto.
    private static String hash(String raw) {
        return "hash:" + raw;
    }

    private static RefreshToken stored(String raw, Instant issuedAt) {
        return RefreshToken.restore(UUID.randomUUID(), hash(raw), USER_ID,
                issuedAt.plus(Duration.ofDays(7)), false);
    }

    @Test
    void shouldRotateTokens() {
        RefreshToken oldToken = stored("old-token", NOW);
        when(refreshTokens.findByTokenHashForUpdate(hash("old-token"))).thenReturn(Optional.of(oldToken));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(accessTokens.issue(user)).thenReturn(new AccessToken("access-token", 900L));
        when(refreshTokenGenerator.generate()).thenReturn("refresh-token");

        AuthenticationResult result = refresh.execute("old-token");

        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens, times(2)).save(captor.capture());
        List<RefreshToken> saved = captor.getAllValues();
        assertThat(saved.get(0)).isSameAs(oldToken);
        assertThat(saved.get(1).getTokenHash()).isEqualTo(hash("refresh-token"));
        assertThat(saved.get(1).getUserId()).isEqualTo(USER_ID);
        assertThat(saved.get(1).isRevoked()).isFalse();
    }

    @Test
    void shouldRejectLockedUserButStillConsumeTheToken() {
        RefreshToken token = stored("locked-token", NOW);
        User locked = User.restore(USER_ID, "john@universum.api.br", "encoded", "John", User.DEFAULT_ROLE,
                User.MAX_FAILED_ATTEMPTS, NOW.plusSeconds(60));
        when(refreshTokens.findByTokenHashForUpdate(hash("locked-token"))).thenReturn(Optional.of(token));
        when(users.findById(USER_ID)).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> refresh.execute("locked-token"))
                .isInstanceOf(UserBlockedException.class);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokens).save(token);
        verify(accessTokens, never()).issue(any());
    }

    @Test
    void shouldFailWhenTokenDoesNotExist() {
        when(refreshTokens.findByTokenHashForUpdate(hash("unknown"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refresh.execute("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid, revoked, or expired.");
    }

    @Test
    void shouldRevokeAllSessionsWhenRevokedTokenIsReused() {
        RefreshToken revoked = stored("reused-token", NOW);
        revoked.revoke();
        when(refreshTokens.findByTokenHashForUpdate(hash("reused-token"))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refresh.execute("reused-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid, revoked, or expired.");

        // Reuso de token já rotacionado: todas as sessões do usuário são encerradas.
        verify(refreshTokens).revokeAllByUserId(USER_ID);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void shouldFailWhenTokenIsExpired() {
        RefreshToken expired = stored("expired-token", NOW.minus(Duration.ofDays(10)));
        when(refreshTokens.findByTokenHashForUpdate(hash("expired-token"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refresh.execute("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid, revoked, or expired.");

        verify(refreshTokens, never()).save(any());
    }

    @Test
    void shouldFailWhenUserNoLongerExists() {
        RefreshToken token = stored("orphan-token", NOW);
        when(refreshTokens.findByTokenHashForUpdate(hash("orphan-token"))).thenReturn(Optional.of(token));
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refresh.execute("orphan-token"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found.");

        assertThat(token.isRevoked()).isFalse();
    }
}
