package br.api.universum.identity.application.usecases.keys;

import br.api.universum.identity.application.abstractions.SigningKeyGenerator;
import br.api.universum.identity.application.abstractions.SigningKeyGenerator.GeneratedKeyPair;
import br.api.universum.identity.application.abstractions.SigningKeyRepository;
import br.api.universum.identity.domain.event.SigningKeyRotated;
import br.api.universum.identity.domain.model.SigningKey;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Substitui a chave de assinatura ativa. Todo access token assinado com a chave anterior deixa de
 * ser aceito; os refresh tokens continuam válidos, então os clientes só precisam fazer refresh.
 *
 * <p>O {@link SigningKeyRotated} só chega aos listeners ({@code @TransactionalEventListener}) depois
 * do commit: uma rotação desfeita não limpa o cache de chaves.</p>
 */
@Service
@RequiredArgsConstructor
public class RotateSigningKeyUseCase {

    private final SigningKeyRepository keys;
    private final SigningKeyGenerator generator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public SigningKeyResponse execute() {
        SigningKey key = keys.findForUpdate().orElseThrow(
                () -> new IllegalStateException("JWT signing key row is missing. Apply Flyway migration V2."));

        GeneratedKeyPair pair = generator.generate();
        key.rotate(UUID.randomUUID(), pair.publicKey(), pair.privateKey(), clock.instant());
        keys.save(key);

        events.publishEvent(new SigningKeyRotated(key.getKeyId()));
        return SigningKeyResponse.from(key);
    }
}
