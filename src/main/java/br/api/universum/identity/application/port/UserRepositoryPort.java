package br.api.universum.identity.application.port;

import br.api.universum.identity.application.dto.PageQuery;
import br.api.universum.identity.application.dto.PageResult;
import br.api.universum.identity.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    PageResult<User> findAll(PageQuery query);

    /** Insere um usuário novo (ainda sem id) ou atualiza um existente. Devolve o estado gravado, com o id. */
    <S extends User> S save(S user);

    void delete(User user);
}
