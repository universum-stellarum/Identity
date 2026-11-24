package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.VerificationKey;
import br.api.universum.identity.application.port.SigningKeyRepositoryPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;

import lombok.RequiredArgsConstructor;

/** Devolve só a metade pública da chave ativa, a que é publicada para validar os tokens. */
@RequiredArgsConstructor
public class GetVerificationKeyUseCase {

    private final SigningKeyRepositoryPort keys;
    private final UnitOfWorkPort unitOfWork;

    public VerificationKey execute() {
        return unitOfWork.read(() -> keys.findActive().map(VerificationKey::from).orElseThrow(
                () -> new IllegalStateException("JWT signing key is not initialized. Check Flyway migrations.")));
    }
}
