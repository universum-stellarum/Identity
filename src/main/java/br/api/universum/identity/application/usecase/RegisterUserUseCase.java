package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.RegisterUserCommand;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.PasswordHasherPort;
import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.application.port.UserRepositoryPort;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final UnitOfWorkPort unitOfWork;

    public UserResult execute(RegisterUserCommand command) {
        return unitOfWork.execute(() -> {
            if (users.existsByEmail(command.email())) {
                throw new EmailAlreadyInUseException();
            }

            User user = User.register(command.email(), passwordHasher.hash(command.password()), command.name());

            return UserResult.from(users.save(user));
        });
    }
}
