package br.api.universum.identity.application.usecases.users;

import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.application.usecases.users.request.RegisterUserRequest;
import br.api.universum.identity.application.usecases.users.response.UserResponse;
import br.api.universum.identity.domain.exception.EmailAlreadyInUseException;
import br.api.universum.identity.domain.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastra um usuário comum, com o papel padrão. */
@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse execute(RegisterUserRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException();
        }

        User user = User.register(request.email(), passwordEncoder.encode(request.password()), request.name());

        return UserResponse.from(users.save(user));
    }
}
