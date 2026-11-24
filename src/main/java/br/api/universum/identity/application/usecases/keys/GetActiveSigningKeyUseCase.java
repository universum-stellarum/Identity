package br.api.universum.identity.application.usecases.keys;

import br.api.universum.identity.application.abstractions.SigningKeyRepository;
import br.api.universum.identity.domain.model.SigningKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Devolve a chave ativa, inclusive a metade privada. Só para assinar; nunca a exponha. */
@Service
@RequiredArgsConstructor
public class GetActiveSigningKeyUseCase {

    private final SigningKeyRepository keys;

    @Transactional(readOnly = true)
    public SigningKey execute() {
        return keys.findActive().orElseThrow(
                () -> new IllegalStateException("JWT signing key is not initialized. Check Flyway migrations."));
    }
}
