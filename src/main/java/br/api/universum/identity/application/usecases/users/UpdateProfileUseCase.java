package br.api.universum.identity.application.usecases.users;

import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.exception.UserNotFoundException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Altera o e-mail e o nome do usuário autenticado. Os papéis (roles) nunca mudam aqui.
 * O id vem do token, não do corpo da requisição.
 */
@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final UserRepository users;

    @Transactional
    public UserResponse execute(UUID userId, UpdateProfileRequest request) {
        User user = users.findById(userId).orElseThrow(UserNotFoundException::new);

        if (!request.email().equals(user.getEmail()) && users.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException();
        }
        user.changeEmail(request.email());
        user.rename(request.name());

        return UserResponse.from(users.save(user));
    }
}
