package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.UpdateUserCommand;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.application.port.UserRepositoryPort;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Altera e-mail, nome e papéis (roles) de qualquer usuário. */
@RequiredArgsConstructor
public class UpdateUserUseCase {

    private final UserRepositoryPort users;
    private final UnitOfWorkPort unitOfWork;

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
