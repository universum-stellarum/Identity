package br.api.universum.identity.application.port;

import br.api.universum.identity.domain.model.User;

/** Issues the signed access token (JWT) that identifies the user to the other services. */
public interface AccessTokenIssuer {

    AccessToken issue(User user);

    record AccessToken(String value, long expiresInSeconds) {
    }
}
