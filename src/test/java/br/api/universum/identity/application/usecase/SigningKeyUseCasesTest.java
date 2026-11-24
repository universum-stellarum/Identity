package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.ImmediateUnitOfWork;
import br.api.universum.identity.application.dto.SigningKeyResult;
import br.api.universum.identity.application.dto.VerificationKey;
import br.api.universum.identity.application.port.DomainEventPublisherPort;
import br.api.universum.identity.application.port.SigningKeyGeneratorPort;
import br.api.universum.identity.application.port.SigningKeyGeneratorPort.GeneratedKeyPair;
import br.api.universum.identity.application.port.SigningKeyRepositoryPort;
import br.api.universum.identity.domain.event.SigningKeyRotated;
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
    private final ImmediateUnitOfWork unitOfWork = new ImmediateUnitOfWork();

    @Mock private SigningKeyRepositoryPort keys;
    @Mock private SigningKeyGeneratorPort generator;
    @Mock private DomainEventPublisherPort events;

    private static SigningKey activeKey() {
        return SigningKey.restore(UUID.randomUUID(), "public", "private", NOW.minusSeconds(3600));
    }

    @Test
    void initializeGeneratesTheFirstPair() {
        SigningKey empty = SigningKey.restore(null, null, null, null);
        when(keys.findForUpdate()).thenReturn(Optional.of(empty));
        when(generator.generate()).thenReturn(new GeneratedKeyPair("public", "private"));

        SigningKey key = new InitializeSigningKeyUseCase(keys, generator, unitOfWork, clock).execute();

        assertThat(key.isInitialized()).isTrue();
        assertThat(key.getRotatedAt()).isEqualTo(NOW);
        verify(keys).save(empty);
    }

    @Test
    void initializeKeepsAnExistingKey() {
        SigningKey existing = activeKey();
        UUID keyId = existing.getKeyId();
        when(keys.findForUpdate()).thenReturn(Optional.of(existing));

        SigningKey key = new InitializeSigningKeyUseCase(keys, generator, unitOfWork, clock).execute();

        assertThat(key.getKeyId()).isEqualTo(keyId);
        verifyNoInteractions(generator);
        verify(keys, never()).save(any());
    }

    @Test
    void initializeFailsWhenTheKeyStorageIsMissing() {
        when(keys.findForUpdate()).thenReturn(Optional.empty());
        var useCase = new InitializeSigningKeyUseCase(keys, generator, unitOfWork, clock);

        assertThatThrownBy(useCase::execute).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("migration V2");
    }

    @Test
    void rotateReplacesTheKeyAndAnnouncesIt() {
        SigningKey existing = activeKey();
        UUID previous = existing.getKeyId();
        when(keys.findForUpdate()).thenReturn(Optional.of(existing));
        when(generator.generate()).thenReturn(new GeneratedKeyPair("new-public", "new-private"));

        SigningKeyResult result = new RotateSigningKeyUseCase(keys, generator, events, unitOfWork, clock).execute();

        assertThat(result.keyId()).isNotEqualTo(previous);
        assertThat(result.algorithm()).isEqualTo("RS256");
        assertThat(result.rotatedAt()).isEqualTo(NOW);
        assertThat(existing.getPrivateKey()).isEqualTo("new-private");
        verify(keys).save(existing);
        verify(events).publish(new SigningKeyRotated(result.keyId()));
    }

    @Test
    void verificationKeyCarriesOnlyThePublicHalf() {
        SigningKey key = activeKey();
        when(keys.findActive()).thenReturn(Optional.of(key));

        VerificationKey verification = new GetVerificationKeyUseCase(keys, unitOfWork).execute();

        assertThat(verification.keyId()).isEqualTo(key.getKeyId());
        assertThat(verification.publicKey()).isEqualTo("public");
        assertThat(verification.algorithm()).isEqualTo("RS256");
    }

    @Test
    void activeKeyFailsWhenNotInitialized() {
        when(keys.findActive()).thenReturn(Optional.empty());
        var useCase = new GetActiveSigningKeyUseCase(keys, unitOfWork);

        assertThatThrownBy(useCase::execute).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialized");
    }
}
