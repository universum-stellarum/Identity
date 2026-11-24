package br.api.universum.identity.application.dto;

import br.api.universum.identity.domain.model.User;

import java.util.UUID;

/** Public view of a user: never carries the password hash or the lock state. */
public record UserResult(UUID id, String email, String name, String roles) {

    public static UserResult from(User user) {
        return new UserResult(user.getId(), user.getEmail(), user.getName(), user.getRoles());
    }
}
