package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.SigningKeyRepositoryPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.domain.model.SigningKey;

import lombok.RequiredArgsConstructor;

/** Devolve a chave ativa, inclusive a metade privada. Só para assinar; nunca a exponha. */
@RequiredArgsConstructor
public class GetActiveSigningKeyUseCase {

    private final SigningKeyRepositoryPort keys;
    private final UnitOfWorkPort unitOfWork;

    public SigningKey execute() {
        return unitOfWork.read(() -> keys.findActive().orElseThrow(
                () -> new IllegalStateException("JWT signing key is not initialized. Check Flyway migrations.")));
    }
}
