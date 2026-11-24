package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.UpdateProfileCommand;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.application.port.UserRepositoryPort;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Altera o e-mail e o nome do usuário autenticado. Os papéis (roles) nunca mudam aqui. */
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final UserRepositoryPort users;
    private final UnitOfWorkPort unitOfWork;

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
