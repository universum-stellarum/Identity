package br.api.universum.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static br.api.universum.identity.domain.model.Guard.requireText;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseEntity<UUID> {

    public static final String DEFAULT_ROLE = "ROLE_USER";
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /**
     * Gerado no primeiro save; {@code null} até lá.
     */
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

    public static User register(String email, String encodedPassword, String name) {
        return new User(
                null,
                requireText(email, "Email is required."),
                requireText(encodedPassword, "Password is required."),
                requireText(name, "Name is required."),
                DEFAULT_ROLE,
                0,
                null
        );
    }

    /**
     * Reconstrói um usuário num estado conhecido, como viria do armazenamento.
     */
    public static User restore(
            UUID id,
            String email,
            String encodedPassword,
            String name,
            String roles,
            int failedAttempts,
            Instant lockExpiredAt) {
        return new User(
                id,
                email,
                encodedPassword,
                name,
                roles,
                failedAttempts,
                lockExpiredAt
        );
    }

    /** {@code true} enquanto o bloqueio por excesso de tentativas de login não tiver expirado. */
    public boolean isLocked(Instant now) {
        return lockExpiredAt != null && lockExpiredAt.isAfter(now);
    }

    /**
     * Conta uma senha errada. Ao chegar a {@link #MAX_FAILED_ATTEMPTS}, bloqueia a conta por
     * {@link #LOCK_DURATION}.
     */
    public void registerFailedLogin(Instant now) {
        this.failedAttempts++;

        if (this.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockExpiredAt = now.plus(LOCK_DURATION);
        }
    }

    /** Zera o contador de tentativas e remove o bloqueio. */
    public void registerSuccessfulLogin() {
        this.failedAttempts = 0;
        this.lockExpiredAt = null;
    }

    public void changeEmail(String email) {
        this.email = requireText(email, "Email is required.");
    }

    public void rename(String name) {
        this.name = requireText(name, "Name is required.");
    }

    public void changeRoles(String roles) {
        this.roles = requireText(roles, "Roles are required.");
    }
}
