package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.domain.exception.UserNotFoundException;

import java.util.UUID;

public class DeleteUserUseCase {

    private final UserRepository users;
    private final UnitOfWork unitOfWork;

    public DeleteUserUseCase(UserRepository users, UnitOfWork unitOfWork) {
        this.users = users;
        this.unitOfWork = unitOfWork;
    }

    public void execute(UUID id) {
        unitOfWork.run(() -> users.delete(users.findById(id).orElseThrow(UserNotFoundException::new)));
    }
}
