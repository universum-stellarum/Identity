package br.api.universum.identity.infrastructure.persistence;

import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.domain.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Implementa o {@link UserRepository} pelo próprio Spring Data: os métodos com a mesma assinatura
 * dos do {@link JpaRepository} ({@code findById}, {@code findAll}, {@code save}, {@code delete}) e as
 * consultas derivadas do nome ({@code findByEmail}, {@code existsByEmail}) são gerados pelo Spring.
 */
public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {
}
