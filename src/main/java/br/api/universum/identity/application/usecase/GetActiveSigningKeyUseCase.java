package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.SigningKeyRepository;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.domain.model.SigningKey;

/** Returns the active key, including the private half. For signing only; never expose it. */
public class GetActiveSigningKeyUseCase {

    private final SigningKeyRepository keys;
    private final UnitOfWork unitOfWork;

    public GetActiveSigningKeyUseCase(SigningKeyRepository keys, UnitOfWork unitOfWork) {
        this.keys = keys;
        this.unitOfWork = unitOfWork;
    }

    public SigningKey execute() {
        return unitOfWork.read(() -> keys.findActive().orElseThrow(
                () -> new IllegalStateException("JWT signing key is not initialized. Check Flyway migrations.")));
    }
}
