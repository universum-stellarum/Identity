package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.UnitOfWorkPort;
import br.api.universum.identity.application.port.UserRepositoryPort;
import br.api.universum.identity.domain.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class DeleteUserUseCase {

    private final UserRepositoryPort users;
    private final UnitOfWorkPort unitOfWork;

    public void execute(UUID id) {
        unitOfWork.run(() -> users.delete(users.findById(id).orElseThrow(UserNotFoundException::new)));
    }
}
