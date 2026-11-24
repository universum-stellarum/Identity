package br.api.universum.identity.application.abstractions;

import br.api.universum.identity.domain.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findAll(Pageable pageable);

    /** Insere um usuário novo (ainda sem id) ou atualiza um existente. Devolve o estado gravado, com o id. */
    <S extends User> S save(S user);

    void delete(User user);
}
