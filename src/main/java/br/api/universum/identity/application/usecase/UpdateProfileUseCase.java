package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.UpdateProfileCommand;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Changes the email and name of the authenticated user. Roles are never changed here. */
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final UserRepository users;
    private final UnitOfWork unitOfWork;

    public UserResult execute(UUID userId, UpdateProfileCommand command) {
        return unitOfWork.execute(() -> {
            User user = users.findById(userId).orElseThrow(UserNotFoundException::new);

            if (!command.email().equals(user.getEmail()) && users.existsByEmail(command.email())) {
                throw new EmailAlreadyInUseException();
            }
            user.changeEmail(command.email());
            user.rename(command.name());

            return UserResult.from(users.save(user));
        });
    }
}
