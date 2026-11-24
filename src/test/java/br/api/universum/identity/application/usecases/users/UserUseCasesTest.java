package br.api.universum.identity.application.usecases.users;

import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.application.usecases.users.request.RegisterUserRequest;
import br.api.universum.identity.application.usecases.users.request.UpdateProfileRequest;
import br.api.universum.identity.application.usecases.users.request.UpdateUserRequest;
import br.api.universum.identity.application.usecases.users.response.UserResponse;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.User;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwordEncoder;

    private static User existingUser() {
        return User.restore(USER_ID, "john@universum.api.br", "encoded", "John", User.DEFAULT_ROLE, 0, null);
    }

    private void saveReturnsArgument() {
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    class Register {

        @Test
        void shouldEncodePasswordAndPersistUserWithDefaultRole() {
            when(users.existsByEmail("john@universum.api.br")).thenReturn(false);
            when(passwordEncoder.encode("secret")).thenReturn("encoded");
            saveReturnsArgument();

            UserResponse response = new RegisterUserUseCase(users, passwordEncoder)
                    .execute(new RegisterUserRequest("john@universum.api.br", "secret", "John"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(users).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
            assertThat(response.email()).isEqualTo("john@universum.api.br");
            assertThat(response.name()).isEqualTo("John");
            assertThat(response.roles()).isEqualTo(User.DEFAULT_ROLE);
        }

        @Test
        void shouldFailWhenEmailAlreadyInUse() {
            when(users.existsByEmail("john@universum.api.br")).thenReturn(true);
            var useCase = new RegisterUserUseCase(users, passwordEncoder);
            var request = new RegisterUserRequest("john@universum.api.br", "secret", "John");

            assertThatThrownBy(() -> useCase.execute(request))
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

            UserResponse response = new GetUserUseCase(users).execute(USER_ID);

            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo("john@universum.api.br");
        }

        @Test
        void shouldFailWhenUserDoesNotExist() {
            when(users.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
            var useCase = new GetUserUseCase(users);

            assertThatThrownBy(() -> useCase.execute(NON_EXISTENT_ID))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found.");
        }
    }

    @Nested
    class ListAll {

        @Test
        void shouldMapAllUsersAndKeepPagination() {
            Pageable pageable = PageRequest.of(0, 20);
            when(users.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                    existingUser(), User.register("mary@universum.api.br", "encoded", "Mary")), pageable, 2));

            Page<UserResponse> page = new ListUsersUseCase(users).execute(pageable);

            assertThat(page.getContent()).extracting(UserResponse::email)
                    .containsExactly("john@universum.api.br", "mary@universum.api.br");
            assertThat(page.getTotalElements()).isEqualTo(2);
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

            UserResponse response = new UpdateUserUseCase(users)
                    .execute(USER_ID, new UpdateUserRequest("new@universum.api.br", "New Name", "ROLE_ADMIN"));

            assertThat(response.email()).isEqualTo("new@universum.api.br");
            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.roles()).isEqualTo("ROLE_ADMIN");
            verify(users).save(user);
        }

        @Test
        void shouldNotCheckEmailUniquenessWhenEmailIsUnchanged() {
            when(users.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
            saveReturnsArgument();

            UserResponse response = new UpdateUserUseCase(users).execute(USER_ID,
                    new UpdateUserRequest("john@universum.api.br", "John Updated", User.DEFAULT_ROLE));

            verify(users, never()).existsByEmail(any());
            assertThat(response.name()).isEqualTo("John Updated");
        }

        @Test
        void shouldFailWhenNewEmailIsAlreadyInUse() {
            User user = existingUser();
            when(users.findById(USER_ID)).thenReturn(Optional.of(user));
            when(users.existsByEmail("taken@universum.api.br")).thenReturn(true);
            var useCase = new UpdateUserUseCase(users);
            var request = new UpdateUserRequest("taken@universum.api.br", "John", User.DEFAULT_ROLE);

            assertThatThrownBy(() -> useCase.execute(USER_ID, request))
                    .isInstanceOf(EmailAlreadyInUseException.class)
                    .hasMessage("Email is already in use.");

            assertThat(user.getEmail()).isEqualTo("john@universum.api.br");
            verify(users, never()).save(any());
        }

        @Test
        void shouldFailWhenUserDoesNotExist() {
            when(users.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
            var useCase = new UpdateUserUseCase(users);
            var request = new UpdateUserRequest("a@b.c", "John", User.DEFAULT_ROLE);

            assertThatThrownBy(() -> useCase.execute(NON_EXISTENT_ID, request))
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

            UserResponse response = new UpdateProfileUseCase(users)
                    .execute(USER_ID, new UpdateProfileRequest("new@universum.api.br", "New Name"));

            assertThat(response.email()).isEqualTo("new@universum.api.br");
            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.roles()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        void shouldFailWhenNewEmailIsAlreadyInUse() {
            when(users.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
            when(users.existsByEmail("taken@universum.api.br")).thenReturn(true);
            var useCase = new UpdateProfileUseCase(users);
            var request = new UpdateProfileRequest("taken@universum.api.br", "John");

            assertThatThrownBy(() -> useCase.execute(USER_ID, request))
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

            new DeleteUserUseCase(users).execute(USER_ID);

            verify(users).delete(user);
        }

        @Test
        void shouldFailWhenUserDoesNotExist() {
            when(users.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
            var useCase = new DeleteUserUseCase(users);

            assertThatThrownBy(() -> useCase.execute(NON_EXISTENT_ID))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found.");

            verify(users, never()).delete(any());
        }
    }
}
