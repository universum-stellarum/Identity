package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.SigningKeyResult;
import br.api.universum.identity.application.port.DomainEventPublisher;
import br.api.universum.identity.application.port.SigningKeyGenerator;
import br.api.universum.identity.application.port.SigningKeyGenerator.GeneratedKeyPair;
import br.api.universum.identity.application.port.SigningKeyRepository;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.domain.event.SigningKeyRotated;
import br.api.universum.identity.domain.model.SigningKey;

import java.time.Clock;
import java.util.UUID;

/**
 * Replaces the active signing key. Every access token signed with the previous key stops being
 * accepted; refresh tokens remain valid, so clients only need to refresh.
 */
public class RotateSigningKeyUseCase {

    private final SigningKeyRepository keys;
    private final SigningKeyGenerator generator;
    private final DomainEventPublisher events;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RotateSigningKeyUseCase(SigningKeyRepository keys,
                                   SigningKeyGenerator generator,
                                   DomainEventPublisher events,
                                   UnitOfWork unitOfWork,
                                   Clock clock) {
        this.keys = keys;
        this.generator = generator;
        this.events = events;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

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
