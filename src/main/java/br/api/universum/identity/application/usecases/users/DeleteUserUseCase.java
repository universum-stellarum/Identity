package br.api.universum.identity.application.usecases.users;

import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.domain.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Remove um usuário pelo id. */
@Service
@RequiredArgsConstructor
public class DeleteUserUseCase {

    private final UserRepository users;

    @Transactional
    public void execute(UUID id) {
        users.delete(users.findById(id).orElseThrow(UserNotFoundException::new));
    }
}
