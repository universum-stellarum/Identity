package br.api.universum.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration VALIDITY = Duration.ofDays(7);
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void issueShouldStoreHashAndCreateNonRevokedTokenExpiringAfterValidity() {
        RefreshToken token = RefreshToken.issue("hash-abc", USER_ID, NOW, VALIDITY);

        assertThat(token.getId()).isNull();
        assertThat(token.getTokenHash()).isEqualTo("hash-abc");
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getExpiryDate()).isEqualTo(NOW.plus(VALIDITY));
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    void issueShouldRequireHashAndUser() {
        assertThatThrownBy(() -> RefreshToken.issue(" ", USER_ID, NOW, VALIDITY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RefreshToken.issue("hash-abc", null, NOW, VALIDITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotBeExpiredBeforeExpiry() {
        RefreshToken token = RefreshToken.issue("hash-abc", USER_ID, NOW, VALIDITY);

        assertThat(token.isExpired(NOW.plus(Duration.ofDays(6)))).isFalse();
    }

    @Test
    void shouldBeExpiredAfterValidity() {
        RefreshToken token = RefreshToken.issue("hash-abc", USER_ID, NOW, VALIDITY);

        assertThat(token.isExpired(NOW.plus(VALIDITY).plusSeconds(1))).isTrue();
    }

    @Test
    void shouldBeExpiredAtExactExpiry() {
        RefreshToken token = RefreshToken.issue("hash-abc", USER_ID, NOW, VALIDITY);

        assertThat(token.isExpired(NOW.plus(VALIDITY))).isTrue();
    }

    @Test
    void revokeShouldMarkTokenAsRevoked() {
        RefreshToken token = RefreshToken.issue("hash-abc", USER_ID, NOW, VALIDITY);

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
    }
}
