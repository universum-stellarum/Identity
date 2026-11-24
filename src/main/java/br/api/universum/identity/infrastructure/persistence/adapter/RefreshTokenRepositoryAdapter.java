package br.api.universum.identity.infrastructure.persistence.adapter;

import br.api.universum.identity.application.port.RefreshTokenRepository;
import br.api.universum.identity.domain.model.RefreshToken;
import br.api.universum.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import br.api.universum.identity.infrastructure.persistence.repository.RefreshTokenJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity;
        if (token.getId() == null) {
            entity = RefreshTokenJpaEntity.newFrom(token);
        } else {
            // Inside the unit of work this returns the instance already loaded (and locked).
            entity = jpa.findById(token.getId()).orElseThrow(
                    () -> new IllegalStateException("Refresh token %s no longer exists.".formatted(token.getId())));
            entity.copyFrom(token);
        }
        return jpa.save(entity).toDomain();
    }

    @Override
    public void revokeAllByUserId(UUID userId) {
        jpa.revokeAllByUserId(userId);
    }
}
