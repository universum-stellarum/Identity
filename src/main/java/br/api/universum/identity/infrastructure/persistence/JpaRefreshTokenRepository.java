package br.api.universum.identity.infrastructure.persistence;

import br.api.universum.identity.application.abstractions.RefreshTokenRepository;
import br.api.universum.identity.domain.model.RefreshToken;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementa o {@link RefreshTokenRepository} pelo próprio Spring Data. Os métodos do port são
 * redeclarados aqui só para receber a consulta e o bloqueio, que são detalhes de JPA.
 */
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshToken, UUID>, RefreshTokenRepository {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Override
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.userId = :userId AND t.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);
}
