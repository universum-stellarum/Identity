package br.api.universum.identity.application.usecases.users;

import br.api.universum.identity.application.abstractions.UserRepository;
import br.api.universum.identity.application.usecases.users.response.UserResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lista os usuários, paginado. */
@Service
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserRepository users;

    @Transactional(readOnly = true)
    public Page<UserResponse> execute(Pageable pageable) {
        return users.findAll(pageable)
                .map(UserResponse::from);
    }
}
