package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.SigningKeyResult;
import br.api.universum.identity.application.port.DomainEventPublisherPort;
import br.api.universum.identity.application.port.SigningKeyGeneratorPort;
import br.api.universum.identity.application.port.SigningKeyGeneratorPort.GeneratedKeyPair;
import br.api.universum.identity.application.port.SigningKeyRepositoryPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.domain.event.SigningKeyRotated;
import br.api.universum.identity.domain.model.SigningKey;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.UUID;

/**
 * Replaces the active signing key. Every access token signed with the previous key stops being
 * accepted; refresh tokens remain valid, so clients only need to refresh.
 */
@RequiredArgsConstructor
public class RotateSigningKeyUseCase {

    private final SigningKeyRepositoryPort keys;
    private final SigningKeyGeneratorPort generator;
    private final DomainEventPublisherPort events;
    private final UnitOfWorkPort unitOfWork;
    private final Clock clock;

    public SigningKeyResult execute() {
        return unitOfWork.execute(() -> {
            SigningKey key = keys.findForUpdate().orElseThrow(
                    () -> new IllegalStateException("JWT signing key row is missing. Apply Flyway migration V2."));

            GeneratedKeyPair pair = generator.generate();
            key.rotate(UUID.randomUUID(), pair.publicKey(), pair.privateKey(), clock.instant());
            keys.save(key);

            events.publish(new SigningKeyRotated(key.getKeyId()));
            return SigningKeyResult.from(key);
        });
    }
}
