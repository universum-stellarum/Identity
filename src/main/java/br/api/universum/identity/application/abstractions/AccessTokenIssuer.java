package br.api.universum.identity.application.abstractions;

import br.api.universum.identity.domain.model.User;

/** Emite o access token assinado (JWT) que identifica o usuário para os outros serviços. */
public interface AccessTokenIssuer {

    /** Gera um access token novo para o usuário, com os dados dele nas claims. */
    AccessToken issue(User user);

    /**
     * @param value            o JWT assinado, pronto para ir no header {@code Authorization: Bearer}
     * @param expiresInSeconds quantos segundos o token vale a partir da emissão
     */
    record AccessToken(String value, long expiresInSeconds) {
    }
}
