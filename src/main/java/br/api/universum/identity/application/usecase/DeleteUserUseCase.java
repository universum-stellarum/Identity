package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.domain.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class DeleteUserUseCase {

    private final UserRepository users;
    private final UnitOfWork unitOfWork;

    public void execute(UUID id) {
        unitOfWork.run(() -> users.delete(users.findById(id).orElseThrow(UserNotFoundException::new)));
    }
}
