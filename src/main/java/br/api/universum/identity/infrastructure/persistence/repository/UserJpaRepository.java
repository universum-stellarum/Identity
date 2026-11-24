package br.api.universum.identity.infrastructure.persistence.repository;

import br.api.universum.identity.application.dto.PageQuery;
import br.api.universum.identity.application.dto.PageResult;
import br.api.universum.identity.application.port.UserRepositoryPort;
import br.api.universum.identity.domain.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Implementa o {@link UserRepositoryPort} pelo próprio Spring Data: os métodos do port com a mesma
 * assinatura dos do {@link JpaRepository} ({@code findById}, {@code save}, {@code delete}) e as
 * consultas derivadas do nome ({@code findByEmail}, {@code existsByEmail}) são gerados pelo Spring.
 */
public interface UserJpaRepository extends JpaRepository<User, UUID>, UserRepositoryPort {

    /** Converte a paginação da aplicação para a do Spring Data e o resultado de volta. */
    @Override
    default PageResult<User> findAll(PageQuery query) {
        Page<User> page = findAll(PageRequest.of(query.page(), query.size(), sortOf(query)));
        return new PageResult<>(page.getContent(), query, page.getTotalElements());
    }

    private static Sort sortOf(PageQuery query) {
        return Sort.by(query.sort().stream()
                .map(order -> order.ascending()
                        ? Sort.Order.asc(order.property())
                        : Sort.Order.desc(order.property()))
                .toList());
    }
}
