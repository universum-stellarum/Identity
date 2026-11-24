package br.api.universum.identity.infrastructure.persistence.repository;

import br.api.universum.identity.domain.model.SigningKey;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SigningKeyJpaRepository extends JpaRepository<SigningKey, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM SigningKey k WHERE k.id = " + SigningKey.SINGLETON_ID)
    Optional<SigningKey> findForUpdate();
}
