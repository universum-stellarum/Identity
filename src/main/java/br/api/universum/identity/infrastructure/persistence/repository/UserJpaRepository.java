package br.api.universum.identity.infrastructure.persistence.repository;

import br.api.universum.identity.infrastructure.persistence.entity.UserJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
