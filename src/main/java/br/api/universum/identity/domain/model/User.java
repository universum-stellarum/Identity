package br.api.universum.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static br.api.universum.identity.domain.model.Guard.requireText;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends AuditableEntity {

    public static final String DEFAULT_ROLE = "ROLE_USER";
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /** Generated on first save; {@code null} until then. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    /** Rebuilds a user in a known state, as it would come from storage. */
    public static User restore(UUID id, String email, String encodedPassword, String name, String roles,
                               int failedAttempts, Instant lockExpiredAt) {
        return new User(id, email, encodedPassword, name, roles, failedAttempts, lockExpiredAt);
    }

    public boolean isLocked(Instant now) {
        return lockExpiredAt != null && lockExpiredAt.isAfter(now);
    }

    public void registerFailedLogin(Instant now) {
        this.failedAttempts++;

        if (this.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockExpiredAt = now.plus(LOCK_DURATION);
        }
    }

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
