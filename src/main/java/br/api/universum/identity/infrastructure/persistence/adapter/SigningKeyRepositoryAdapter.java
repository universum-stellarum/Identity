package br.api.universum.identity.infrastructure.persistence.adapter;

import br.api.universum.identity.application.port.SigningKeyRepository;
import br.api.universum.identity.domain.model.SigningKey;
import br.api.universum.identity.infrastructure.persistence.repository.SigningKeyJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SigningKeyRepositoryAdapter implements SigningKeyRepository {

    private final SigningKeyJpaRepository jpa;

    @Override
    public Optional<SigningKey> findActive() {
        return jpa.findById(SigningKey.SINGLETON_ID).filter(SigningKey::isInitialized);
    }

    @Override
    public Optional<SigningKey> findForUpdate() {
        return jpa.findForUpdate();
    }

    @Override
    public void save(SigningKey key) {
        jpa.save(key);
    }
}
