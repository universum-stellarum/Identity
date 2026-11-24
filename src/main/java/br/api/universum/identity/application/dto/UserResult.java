package br.api.universum.identity.application.dto;

import br.api.universum.identity.domain.model.User;

import java.util.UUID;

/** Visão pública de um usuário: nunca carrega o hash da senha nem o estado de bloqueio. */
public record UserResult(UUID id, String email, String name, String roles) {

    public static UserResult from(User user) {
        return new UserResult(user.getId(), user.getEmail(), user.getName(), user.getRoles());
    }
}
