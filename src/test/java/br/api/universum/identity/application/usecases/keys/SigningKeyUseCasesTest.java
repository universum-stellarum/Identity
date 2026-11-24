package br.api.universum.identity.application.usecases.keys;

import br.api.universum.identity.application.abstractions.SigningKeyGenerator;
import br.api.universum.identity.application.abstractions.SigningKeyGenerator.GeneratedKeyPair;
import br.api.universum.identity.application.abstractions.SigningKeyRepository;
import br.api.universum.identity.domain.event.SigningKeyRotated;
import br.api.universum.identity.domain.model.SigningKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    @Mock private ApplicationEventPublisher events;

    private static SigningKey activeKey() {
        return SigningKey.restore(UUID.randomUUID(), "public", "private", NOW.minusSeconds(3600));
    }

    @Test
    void initializeGeneratesTheFirstPair() {
        SigningKey empty = SigningKey.restore(null, null, null, null);
        when(keys.findForUpdate()).thenReturn(Optional.of(empty));
        when(generator.generate()).thenReturn(new GeneratedKeyPair("public", "private"));

        SigningKey key = new InitializeSigningKeyUseCase(keys, generator, clock).execute();

        assertThat(key.isInitialized()).isTrue();
        assertThat(key.getRotatedAt()).isEqualTo(NOW);
        verify(keys).save(empty);
    }

    @Test
    void initializeKeepsAnExistingKey() {
        SigningKey existing = activeKey();
        UUID keyId = existing.getKeyId();
        when(keys.findForUpdate()).thenReturn(Optional.of(existing));

        SigningKey key = new InitializeSigningKeyUseCase(keys, generator, clock).execute();

        assertThat(key.getKeyId()).isEqualTo(keyId);
        verifyNoInteractions(generator);
        verify(keys, never()).save(any());
    }

    @Test
    void initializeFailsWhenTheKeyStorageIsMissing() {
        when(keys.findForUpdate()).thenReturn(Optional.empty());
        var useCase = new InitializeSigningKeyUseCase(keys, generator, clock);

        assertThatThrownBy(useCase::execute).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("migration V2");
    }

    @Test
    void rotateReplacesTheKeyAndAnnouncesIt() {
        SigningKey existing = activeKey();
        UUID previous = existing.getKeyId();
        when(keys.findForUpdate()).thenReturn(Optional.of(existing));
        when(generator.generate()).thenReturn(new GeneratedKeyPair("new-public", "new-private"));

        SigningKeyResponse response = new RotateSigningKeyUseCase(keys, generator, events, clock).execute();

        assertThat(response.keyId()).isNotEqualTo(previous);
        assertThat(response.algorithm()).isEqualTo("RS256");
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(existing.getPrivateKey()).isEqualTo("new-private");
        verify(keys).save(existing);
        verify(events).publishEvent(new SigningKeyRotated(response.keyId()));
    }

    @Test
    void verificationKeyCarriesOnlyThePublicHalf() {
        SigningKey key = activeKey();
        when(keys.findActive()).thenReturn(Optional.of(key));

        VerificationKeyResponse verification = new GetVerificationKeyUseCase(keys).execute();

        assertThat(verification.keyId()).isEqualTo(key.getKeyId());
        assertThat(verification.publicKey()).isEqualTo("public");
        assertThat(verification.algorithm()).isEqualTo("RS256");
    }

    @Test
    void activeKeyFailsWhenNotInitialized() {
        when(keys.findActive()).thenReturn(Optional.empty());
        var useCase = new GetActiveSigningKeyUseCase(keys);

        assertThatThrownBy(useCase::execute).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialized");
    }
}
