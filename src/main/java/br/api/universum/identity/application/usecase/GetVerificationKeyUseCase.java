package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.VerificationKey;
import br.api.universum.identity.application.port.SigningKeyRepository;
import br.api.universum.identity.application.port.UnitOfWork;

import lombok.RequiredArgsConstructor;

/** Returns only the public half of the active key, the one published for token verification. */
@RequiredArgsConstructor
public class GetVerificationKeyUseCase {

    private final SigningKeyRepository keys;
    private final UnitOfWork unitOfWork;

    public VerificationKey execute() {
        return unitOfWork.read(() -> keys.findActive().map(VerificationKey::from).orElseThrow(
                () -> new IllegalStateException("JWT signing key is not initialized. Check Flyway migrations.")));
    }
}
