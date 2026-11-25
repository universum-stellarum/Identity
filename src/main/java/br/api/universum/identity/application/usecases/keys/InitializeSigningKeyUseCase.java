package br.api.universum.identity.application.usecases.keys;

import br.api.universum.identity.application.abstractions.SigningKeyGenerator;
import br.api.universum.identity.application.abstractions.SigningKeyGenerator.GeneratedKeyPair;
import br.api.universum.identity.application.abstractions.SigningKeyRepository;
import br.api.universum.identity.domain.model.SigningKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Devolve a chave de assinatura, gerando o par na primeira vez em que o serviço sobe. Depois disso,
 * a mesma chave é reaproveitada em todas as subidas.
 */
@Service
@RequiredArgsConstructor
public class InitializeSigningKeyUseCase {

    private final SigningKeyRepository keys;
    private final SigningKeyGenerator generator;
    private final Clock clock;

    @Transactional
    public SigningKey execute() {
        SigningKey key = keys.find().orElseThrow(
                () -> new IllegalStateException("JWT signing key row is missing. Apply Flyway migration V2.")
        );

        if (!key.isInitialized()) {
            GeneratedKeyPair pair = generator.generate();

            key.initialize(
                    UUID.randomUUID(),
                    pair.publicKey(),
                    pair.privateKey(),
                    clock.instant()
            );

            keys.save(key);
        }
        return key;
    }
}
