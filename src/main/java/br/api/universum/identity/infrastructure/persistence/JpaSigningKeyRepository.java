package br.api.universum.identity.infrastructure.persistence;

import br.api.universum.identity.application.abstractions.SigningKeyRepository;
import br.api.universum.identity.domain.model.SigningKey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Implementa o {@link SigningKeyRepository} pelo próprio Spring Data. */
public interface JpaSigningKeyRepository extends JpaRepository<SigningKey, Integer>, SigningKeyRepository {

    @Override
    default Optional<SigningKey> find() {
        return findById(SigningKey.SINGLETON_ID);
    }
}
