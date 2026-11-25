package br.api.universum.identity.application.usecases.keys;

import br.api.universum.identity.application.abstractions.SigningKeyGenerator.GeneratedKeyPair;
import br.api.universum.identity.application.abstractions.SigningKeyGenerator;
import br.api.universum.identity.application.abstractions.SigningKeyRepository;
import br.api.universum.identity.domain.model.SigningKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SigningKeyUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock private SigningKeyRepository keys;
    @Mock private SigningKeyGenerator generator;

    @Test
    void initializeGeneratesTheFirstPair() {
        SigningKey empty = SigningKey.restore(null, null, null, null);
        when(keys.find()).thenReturn(Optional.of(empty));
        when(generator.generate()).thenReturn(new GeneratedKeyPair("public", "private"));

        SigningKey key = new InitializeSigningKeyUseCase(keys, generator, clock).execute();

        assertThat(key.isInitialized()).isTrue();
        assertThat(key.getRotatedAt()).isEqualTo(NOW);
        verify(keys).save(empty);
    }

    @Test
    void initializeKeepsAnExistingKey() {
        SigningKey existing = SigningKey.restore(UUID.randomUUID(), "public", "private", NOW.minusSeconds(3600));
        UUID keyId = existing.getKeyId();
        when(keys.find()).thenReturn(Optional.of(existing));

        SigningKey key = new InitializeSigningKeyUseCase(keys, generator, clock).execute();

        assertThat(key.getKeyId()).isEqualTo(keyId);
        verifyNoInteractions(generator);
        verify(keys, never()).save(any());
    }

    @Test
    void initializeFailsWhenTheKeyStorageIsMissing() {
        when(keys.find()).thenReturn(Optional.empty());
        var useCase = new InitializeSigningKeyUseCase(keys, generator, clock);

        assertThatThrownBy(useCase::execute).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("migration V2");
    }
}
