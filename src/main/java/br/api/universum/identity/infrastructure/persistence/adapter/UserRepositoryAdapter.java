package br.api.universum.identity.infrastructure.persistence.adapter;

import br.api.universum.identity.application.dto.PageQuery;
import br.api.universum.identity.application.dto.PageResult;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.domain.model.User;
import br.api.universum.identity.infrastructure.persistence.entity.UserJpaEntity;
import br.api.universum.identity.infrastructure.persistence.repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public PageResult<User> findAll(PageQuery query) {
        Page<UserJpaEntity> page = jpa.findAll(PageRequest.of(query.page(), query.size(), sortOf(query)));
        return new PageResult<>(page.map(UserJpaEntity::toDomain).getContent(), query, page.getTotalElements());
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity;
        if (user.getId() == null) {
            entity = UserJpaEntity.newFrom(user);
        } else {
            // Inside the unit of work this returns the instance already loaded, without a new query.
            entity = jpa.findById(user.getId()).orElseThrow(
                    () -> new IllegalStateException("User %s no longer exists.".formatted(user.getId())));
            entity.copyFrom(user);
        }
        return jpa.save(entity).toDomain();
    }

    @Override
    public void delete(User user) {
        jpa.deleteById(user.getId());
    }

    private static Sort sortOf(PageQuery query) {
        return Sort.by(query.sort().stream()
                .map(order -> order.ascending()
                        ? Sort.Order.asc(order.property())
                        : Sort.Order.desc(order.property()))
                .toList());
    }
}
