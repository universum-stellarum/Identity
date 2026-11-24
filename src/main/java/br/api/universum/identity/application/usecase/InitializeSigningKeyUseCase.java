package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.SigningKeyGenerator;
import br.api.universum.identity.application.port.SigningKeyGenerator.GeneratedKeyPair;
import br.api.universum.identity.application.port.SigningKeyRepository;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.domain.model.SigningKey;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.UUID;

/**
 * Returns the active signing key, generating the first pair if none exists yet. Called at startup;
 * the lock guarantees that instances starting together end up with the same key.
 */
@RequiredArgsConstructor
public class InitializeSigningKeyUseCase {

    private final SigningKeyRepository keys;
    private final SigningKeyGenerator generator;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public SigningKey execute() {
        return unitOfWork.execute(() -> {
            SigningKey key = keys.findForUpdate().orElseThrow(
                    () -> new IllegalStateException("JWT signing key row is missing. Apply Flyway migration V2."));

            if (!key.isInitialized()) {
                GeneratedKeyPair pair = generator.generate();
                key.rotate(UUID.randomUUID(), pair.publicKey(), pair.privateKey(), clock.instant());
                keys.save(key);
            }
            return key;
        });
    }
}
