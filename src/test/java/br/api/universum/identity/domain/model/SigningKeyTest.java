package br.api.universum.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SigningKeyTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void emptyKeyIsNotInitializedUntilRotated() {
        SigningKey key = SigningKey.restore(null, null, null, null);
        assertThat(key.isInitialized()).isFalse();

        UUID keyId = UUID.randomUUID();
        key.rotate(keyId, "public", "private", NOW);

        assertThat(key.isInitialized()).isTrue();
        assertThat(key.getKeyId()).isEqualTo(keyId);
        assertThat(key.getRotatedAt()).isEqualTo(NOW);
    }

    @Test
    void rotationRequiresTheCompletePair() {
        SigningKey key = SigningKey.restore(null, null, null, null);

        assertThatThrownBy(() -> key.rotate(UUID.randomUUID(), "public", " ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(key.isInitialized()).isFalse();
    }
}
