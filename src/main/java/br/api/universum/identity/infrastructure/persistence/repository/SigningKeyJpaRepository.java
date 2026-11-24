package br.api.universum.identity.infrastructure.persistence.repository;

import br.api.universum.identity.infrastructure.persistence.entity.SigningKeyJpaEntity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SigningKeyJpaRepository extends JpaRepository<SigningKeyJpaEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM SigningKeyJpaEntity k WHERE k.id = " + SigningKeyJpaEntity.SINGLETON_ID)
    Optional<SigningKeyJpaEntity> findForUpdate();
}
