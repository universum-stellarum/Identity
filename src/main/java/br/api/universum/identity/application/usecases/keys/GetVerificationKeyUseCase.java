package br.api.universum.identity.application.usecases.keys;

import br.api.universum.identity.application.abstractions.SigningKeyRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Devolve só a metade pública da chave ativa, a que é publicada para validar os tokens. */
@Service
@RequiredArgsConstructor
public class GetVerificationKeyUseCase {

    private final SigningKeyRepository keys;

    @Transactional(readOnly = true)
    public VerificationKeyResponse execute() {
        return keys.findActive().map(VerificationKeyResponse::from).orElseThrow(
                () -> new IllegalStateException("JWT signing key is not initialized. Check Flyway migrations."));
    }
}
