package br.api.universum.identity.presentation.dto.response;

import br.api.universum.identity.application.dto.UserResult;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String roles) {

    public static UserResponse from(UserResult user) {
        return new UserResponse(user.id(), user.email(), user.name(), user.roles());
    }
}
