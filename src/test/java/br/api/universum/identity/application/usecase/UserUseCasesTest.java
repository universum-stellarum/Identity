package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.ImmediateUnitOfWork;
import br.api.universum.identity.application.dto.PageQuery;
import br.api.universum.identity.application.dto.PageResult;
import br.api.universum.identity.application.dto.RegisterUserCommand;
import br.api.universum.identity.application.dto.UpdateProfileCommand;
import br.api.universum.identity.application.dto.UpdateUserCommand;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.PasswordHasherPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.application.port.UserRepositoryPort;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.User;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUseCasesTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID NON_EXISTENT_ID = UUID.randomUUID();

    private final UnitOfWorkPort unitOfWork = new ImmediateUnitOfWork();

    @Mock private UserRepositoryPort users;
    @Mock private PasswordHasherPort passwordHasher;

    private static User existingUser() {
        return User.restore(USER_ID, "john@universum.api.br", "encoded", "John", User.DEFAULT_ROLE, 0, null);
    }

    private void saveReturnsArgument() {
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    class Register {

        @Test
        void shouldHashPasswordAndPersistUserWithDefaultRole() {
            when(users.existsByEmail("john@universum.api.br")).thenReturn(false);
            when(passwordHasher.hash("secret")).thenReturn("encoded");
            saveReturnsArgument();

            UserResult result = new RegisterUserUseCase(users, passwordHasher, unitOfWork)
                    .execute(new RegisterUserCommand("john@universum.api.br", "secret", "John"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(users).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
            assertThat(result.email()).isEqualTo("john@universum.api.br");
            assertThat(result.name()).isEqualTo("John");
            assertThat(result.roles()).isEqualTo(User.DEFAULT_ROLE);
        }

        @Test
        void shouldFailWhenEmailAlreadyInUse() {
            when(users.existsByEmail("john@universum.api.br")).thenReturn(true);
            var useCase = new RegisterUserUseCase(users, passwordHasher, unitOfWork);
            var command = new RegisterUserCommand("john@universum.api.br", "secret", "John");

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(EmailAlreadyInUseException.class)
                    .hasMessage("Email is already in use.");

            verify(users, never()).save(any());
        }
    }

    @Nested
    class Get {

        @Test
        void shouldReturnUser() {
            when(users.findById(USER_ID)).thenReturn(Optional.of(existingUser()));

            UserResult result = new GetUserUseCase(users, unitOfWork).execute(USER_ID);

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.email()).isEqualTo("john@universum.api.br");
        }

        @Test
        void shouldFailWhenUserDoesNotExist() {
            when(users.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
            var useCase = new GetUserUseCase(users, unitOfWork);

            assertThatThrownBy(() -> useCase.execute(NON_EXISTENT_ID))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found.");
        }
    }

    @Nested
    class ListAll {

        @Test
        void shouldMapAllUsersAndKeepPagination() {
            PageQuery query = new PageQuery(0, 20, List.of());
            when(users.findAll(query)).thenReturn(new PageResult<>(List.of(
                    existingUser(), User.register("mary@universum.api.br", "encoded", "Mary")), query, 2));

            PageResult<UserResult> page = new ListUsersUseCase(users, unitOfWork).execute(query);

            assertThat(page.content()).extracting(UserResult::email)
                    .containsExactly("john@universum.api.br", "mary@universum.api.br");
            assertThat(page.totalElements()).isEqualTo(2);
        }
    }

    @Nested
    class Update {

        @Test
        void shouldChangeEmailNameAndRoles() {
            User user = existingUser();
            when(users.findById(USER_ID)).thenReturn(Optional.of(user));
            when(users.existsByEmail("new@universum.api.br")).thenReturn(false);
            saveReturnsArgument();

            UserResult result = new UpdateUserUseCase(users, unitOfWork)
                    .execute(USER_ID, new UpdateUserCommand("new@universum.api.br", "New Name", "ROLE_ADMIN"));

            assertThat(result.email()).isEqualTo("new@universum.api.br");
            assertThat(result.name()).isEqualTo("New Name");
            assertThat(result.roles()).isEqualTo("ROLE_ADMIN");
            verify(users).save(user);
        }

        @Test
        void shouldNotCheckEmailUniquenessWhenEmailIsUnchanged() {
            when(users.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
            saveReturnsArgument();

            UserResult result = new UpdateUserUseCase(users, unitOfWork).execute(USER_ID,
                    new UpdateUserCommand("john@universum.api.br", "John Updated", User.DEFAULT_ROLE));

            verify(users, never()).existsByEmail(any());
            assertThat(result.name()).isEqualTo("John Updated");
        }

        @Test
        void shouldFailWhenNewEmailIsAlreadyInUse() {
            User user = existingUser();
            when(users.findById(USER_ID)).thenReturn(Optional.of(user));
            when(users.existsByEmail("taken@universum.api.br")).thenReturn(true);
            var useCase = new UpdateUserUseCase(users, unitOfWork);
            var command = new UpdateUserCommand("taken@universum.api.br", "John", User.DEFAULT_ROLE);

            assertThatThrownBy(() -> useCase.execute(USER_ID, command))
                    .isInstanceOf(EmailAlreadyInUseException.class)
                    .hasMessage("Email is already in use.");

            assertThat(user.getEmail()).isEqualTo("john@universum.api.br");
            verify(users, never()).save(any());
        }

        @Test
        void shouldFailWhenUserDoesNotExist() {
            when(users.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
            var useCase = new UpdateUserUseCase(users, unitOfWork);
            var command = new UpdateUserCommand("a@b.c", "John", User.DEFAULT_ROLE);

            assertThatThrownBy(() -> useCase.execute(NON_EXISTENT_ID, command))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found.");
        }
    }

    @Nested
    class UpdateProfile {

        @Test
        void shouldChangeEmailAndNameButNeverRoles() {
            User user = User.restore(USER_ID, "john@universum.api.br", "encoded", "John", "ROLE_ADMIN", 0, null);
            when(users.findById(USER_ID)).thenReturn(Optional.of(user));
            when(users.existsByEmail("new@universum.api.br")).thenReturn(false);
            saveReturnsArgument();

            UserResult result = new UpdateProfileUseCase(users, unitOfWork)
                    .execute(USER_ID, new UpdateProfileCommand("new@universum.api.br", "New Name"));

            assertThat(result.email()).isEqualTo("new@universum.api.br");
            assertThat(result.name()).isEqualTo("New Name");
            assertThat(result.roles()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        void shouldFailWhenNewEmailIsAlreadyInUse() {
            when(users.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
            when(users.existsByEmail("taken@universum.api.br")).thenReturn(true);
            var useCase = new UpdateProfileUseCase(users, unitOfWork);
            var command = new UpdateProfileCommand("taken@universum.api.br", "John");

            assertThatThrownBy(() -> useCase.execute(USER_ID, command))
                    .isInstanceOf(EmailAlreadyInUseException.class);
            verify(users, never()).save(any());
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldRemoveExistingUser() {
            User user = existingUser();
            when(users.findById(USER_ID)).thenReturn(Optional.of(user));

            new DeleteUserUseCase(users, unitOfWork).execute(USER_ID);

            verify(users).delete(user);
        }

        @Test
        void shouldFailWhenUserDoesNotExist() {
            when(users.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
            var useCase = new DeleteUserUseCase(users, unitOfWork);

            assertThatThrownBy(() -> useCase.execute(NON_EXISTENT_ID))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found.");

            verify(users, never()).delete(any());
        }
    }
}
