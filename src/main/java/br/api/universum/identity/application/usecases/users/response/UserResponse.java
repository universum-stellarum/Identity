package br.api.universum.identity.application.usecases.users.response;

import br.api.universum.identity.domain.model.User;

import java.util.UUID;

/** Visão pública de um usuário: nunca carrega o hash da senha nem o estado de bloqueio. */
public record UserResponse(
        UUID id,
        String email,
        String name,
        String roles) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRoles());
    }
}
