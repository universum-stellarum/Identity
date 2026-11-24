package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.UpdateUserCommand;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Changes email, name and roles of any user. */
@RequiredArgsConstructor
public class UpdateUserUseCase {

    private final UserRepository users;
    private final UnitOfWork unitOfWork;

    public UserResult execute(UUID id, UpdateUserCommand command) {
        return unitOfWork.execute(() -> {
            User user = users.findById(id).orElseThrow(UserNotFoundException::new);

            if (!command.email().equals(user.getEmail())) {
                if (users.existsByEmail(command.email())) {
                    throw new EmailAlreadyInUseException();
                }
                user.changeEmail(command.email());
            }
            user.rename(command.name());
            user.changeRoles(command.roles());

            return UserResult.from(users.save(user));
        });
    }
}
