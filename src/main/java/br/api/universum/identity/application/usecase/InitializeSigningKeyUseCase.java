package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.SigningKeyGeneratorPort;
import br.api.universum.identity.application.port.SigningKeyGeneratorPort.GeneratedKeyPair;
import br.api.universum.identity.application.port.SigningKeyRepositoryPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.domain.model.SigningKey;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.UUID;

/**
 * Devolve a chave de assinatura ativa e gera o primeiro par se ainda não houver nenhum. Chamado na
 * inicialização; o bloqueio garante que instâncias subindo ao mesmo tempo fiquem com a mesma chave.
 */
@RequiredArgsConstructor
public class InitializeSigningKeyUseCase {

    private final SigningKeyRepositoryPort keys;
    private final SigningKeyGeneratorPort generator;
    private final UnitOfWorkPort unitOfWork;
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
