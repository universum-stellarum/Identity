package br.api.universum.identity.application.abstractions;

import br.api.universum.identity.domain.model.User;

/** Emite o access token assinado (JWT) que identifica o usuário para os outros serviços. */
public interface AccessTokenIssuer {

    AccessToken issue(User user);

    record AccessToken(String value, long expiresInSeconds) {
    }
}
