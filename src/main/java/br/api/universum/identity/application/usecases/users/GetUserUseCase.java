package br.api.universum.identity.application.usecases.users;

import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.application.usecases.users.response.UserResponse;
import br.api.universum.identity.domain.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Busca um usuário pelo id. */
@Service
@RequiredArgsConstructor
public class GetUserUseCase {

    private final UserRepository users;

    @Transactional(readOnly = true)
    public UserResponse execute(UUID id) {
        return users.findById(id)
                .map(UserResponse::from)
                .orElseThrow(UserNotFoundException::new);
    }
}
