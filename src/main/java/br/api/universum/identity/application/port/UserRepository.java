package br.api.universum.identity.application.port;

import br.api.universum.identity.application.dto.PageQuery;
import br.api.universum.identity.application.dto.PageResult;
import br.api.universum.identity.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    PageResult<User> findAll(PageQuery query);

    /** Inserts a new user (no id yet) or updates an existing one. Returns the stored state, with the id. */
    User save(User user);

    void delete(User user);
}
