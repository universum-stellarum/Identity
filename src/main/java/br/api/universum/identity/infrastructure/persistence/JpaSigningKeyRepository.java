package br.api.universum.identity.infrastructure.persistence;

import br.api.universum.identity.application.abstractions.SigningKeyRepository;
import br.api.universum.identity.domain.model.SigningKey;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Implementa o {@link SigningKeyRepository} pelo próprio Spring Data. */
public interface JpaSigningKeyRepository extends JpaRepository<SigningKey, Integer>, SigningKeyRepository {

    @Override
    default Optional<SigningKey> findActive() {
        return findById(SigningKey.SINGLETON_ID).filter(SigningKey::isInitialized);
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM SigningKey k WHERE k.id = " + SigningKey.SINGLETON_ID)
    Optional<SigningKey> findForUpdate();
}
