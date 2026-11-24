package br.api.universum.identity.infrastructure.persistence.entity;

import br.api.universum.identity.domain.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity extends AuditableJpaEntity<UUID> {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String roles;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "lock_expired_at")
    private Instant lockExpiredAt;

    public static UserJpaEntity newFrom(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.copyFrom(user);
        return entity;
    }

    /** Copies the domain state onto this entity. The id is never changed. */
    public void copyFrom(User user) {
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.name = user.getName();
        this.roles = user.getRoles();
        this.failedAttempts = user.getFailedAttempts();
        this.lockExpiredAt = user.getLockExpiredAt();
    }

    public User toDomain() {
        return User.restore(id, email, password, name, roles, failedAttempts, lockExpiredAt);
    }
}
