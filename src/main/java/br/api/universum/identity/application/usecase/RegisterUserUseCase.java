package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.RegisterUserCommand;
import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.PasswordHasher;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.model.User;

public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final UnitOfWork unitOfWork;

    public RegisterUserUseCase(UserRepository users, PasswordHasher passwordHasher, UnitOfWork unitOfWork) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.unitOfWork = unitOfWork;
    }

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
