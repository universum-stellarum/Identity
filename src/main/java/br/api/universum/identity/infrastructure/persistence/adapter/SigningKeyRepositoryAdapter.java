package br.api.universum.identity.infrastructure.persistence.adapter;

import br.api.universum.identity.application.port.SigningKeyRepository;
import br.api.universum.identity.domain.model.SigningKey;
import br.api.universum.identity.infrastructure.persistence.entity.SigningKeyJpaEntity;
import br.api.universum.identity.infrastructure.persistence.repository.SigningKeyJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static br.api.universum.identity.infrastructure.persistence.entity.SigningKeyJpaEntity.SINGLETON_ID;

@Component
@RequiredArgsConstructor
public class SigningKeyRepositoryAdapter implements SigningKeyRepository {

    private final SigningKeyJpaRepository jpa;

    @Override
    public Optional<SigningKey> findActive() {
        return jpa.findById(SINGLETON_ID)
                .map(SigningKeyJpaEntity::toDomain)
                .filter(SigningKey::isInitialized);
    }

    @Override
    public Optional<SigningKey> findForUpdate() {
        return jpa.findForUpdate().map(SigningKeyJpaEntity::toDomain);
    }

    @Override
    public void save(SigningKey key) {
        SigningKeyJpaEntity entity = jpa.findById(SINGLETON_ID).orElseThrow(
                () -> new IllegalStateException("JWT signing key row is missing. Apply Flyway migration V2."));
        entity.copyFrom(key);
        jpa.save(entity);
    }
}
