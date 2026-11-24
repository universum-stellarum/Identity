package br.api.universum.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private User newUser() {
        return User.register("john@universum.api.br", "encoded", "John");
    }

    @Test
    void registerShouldCreateUserWithDefaultRoleAndNoFailedAttempts() {
        User user = newUser();

        assertThat(user.getId()).isNull();
        assertThat(user.getEmail()).isEqualTo("john@universum.api.br");
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getName()).isEqualTo("John");
        assertThat(user.getRoles()).isEqualTo(User.DEFAULT_ROLE);
        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockExpiredAt()).isNull();
        assertThat(user.isLocked(NOW)).isFalse();
    }

    @Test
    void registerShouldRejectBlankFields() {
        assertThatThrownBy(() -> User.register(" ", "encoded", "John"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Email is required.");
        assertThatThrownBy(() -> User.register("john@universum.api.br", null, "John"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Password is required.");
        assertThatThrownBy(() -> User.register("john@universum.api.br", "encoded", ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Name is required.");
    }

    @Test
    void restoreShouldKeepTheStoredState() {
        UUID id = UUID.randomUUID();
        Instant lock = NOW.plusSeconds(60);

        User user = User.restore(id, "john@universum.api.br", "encoded", "John", "ROLE_ADMIN", 5, lock);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getRoles()).isEqualTo("ROLE_ADMIN");
        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.isLocked(NOW)).isTrue();
    }

    @Test
    void failedLoginsBelowLimitShouldNotLockAccount() {
        User user = newUser();

        for (int i = 0; i < User.MAX_FAILED_ATTEMPTS - 1; i++) {
            user.registerFailedLogin(NOW);
        }

        assertThat(user.getFailedAttempts()).isEqualTo(User.MAX_FAILED_ATTEMPTS - 1);
        assertThat(user.isLocked(NOW)).isFalse();
    }

    @Test
    void reachingFailedLoginLimitShouldLockAccountForFifteenMinutes() {
        User user = newUser();

        for (int i = 0; i < User.MAX_FAILED_ATTEMPTS; i++) {
            user.registerFailedLogin(NOW);
        }

        assertThat(user.getLockExpiredAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
        assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(16)))).isFalse();
    }

    @Test
    void successfulLoginShouldResetFailedAttemptsAndLock() {
        User user = newUser();
        for (int i = 0; i < User.MAX_FAILED_ATTEMPTS; i++) {
            user.registerFailedLogin(NOW);
        }

        user.registerSuccessfulLogin();

        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockExpiredAt()).isNull();
        assertThat(user.isLocked(NOW)).isFalse();
    }

    @Test
    void changeEmailRenameAndChangeRolesShouldUpdateFields() {
        User user = newUser();

        user.changeEmail("new@universum.api.br");
        user.rename("New Name");
        user.changeRoles("ROLE_ADMIN");

        assertThat(user.getEmail()).isEqualTo("new@universum.api.br");
        assertThat(user.getName()).isEqualTo("New Name");
        assertThat(user.getRoles()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void changesShouldRejectBlankValuesAndKeepPreviousState() {
        User user = newUser();

        assertThatThrownBy(() -> user.changeEmail(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> user.rename(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> user.changeRoles("")).isInstanceOf(IllegalArgumentException.class);

        assertThat(user.getEmail()).isEqualTo("john@universum.api.br");
        assertThat(user.getName()).isEqualTo("John");
        assertThat(user.getRoles()).isEqualTo(User.DEFAULT_ROLE);
    }
}
