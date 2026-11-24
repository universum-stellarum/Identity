package br.api.universum.identity.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static br.api.universum.identity.domain.model.Guard.requireText;

public class User {

    public static final String DEFAULT_ROLE = "ROLE_USER";
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /** Assigned by the persistence layer; {@code null} until the user is saved for the first time. */
    private final UUID id;
    private String email;
    private final String password;
    private String name;
    private String roles;
    private int failedAttempts;
    private Instant lockExpiredAt;

    private User(UUID id, String email, String password, String name, String roles,
                 int failedAttempts, Instant lockExpiredAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.roles = roles;
        this.failedAttempts = failedAttempts;
        this.lockExpiredAt = lockExpiredAt;
    }

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

    /** Rebuilds a user that was already persisted. The stored state is trusted as is. */
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

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getRoles() {
        return roles;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockExpiredAt() {
        return lockExpiredAt;
    }
}
