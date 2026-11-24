package br.api.universum.identity.application.usecases.users;

import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Altera e-mail, nome e papéis (roles) de qualquer usuário. */
@Service
@RequiredArgsConstructor
public class UpdateUserUseCase {

    private final UserRepository users;

    @Transactional
    public UserResponse execute(UUID id, UpdateUserRequest request) {
        User user = users.findById(id).orElseThrow(UserNotFoundException::new);

        if (!request.email().equals(user.getEmail())) {
            if (users.existsByEmail(request.email())) {
                throw new EmailAlreadyInUseException();
            }
            user.changeEmail(request.email());
        }
        user.rename(request.name());
        user.changeRoles(request.roles());

        return UserResponse.from(users.save(user));
    }
}
