package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.dto.UserResult;
import br.api.universum.identity.application.port.UnitOfWork;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.domain.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class GetUserUseCase {

    private final UserRepository users;
    private final UnitOfWork unitOfWork;

    public UserResult execute(UUID id) {
        return unitOfWork.read(() -> users.findById(id)
                .map(UserResult::from)
                .orElseThrow(UserNotFoundException::new));
    }
}
