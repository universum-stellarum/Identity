package br.api.universum.identity.application.abstractions;

/** Gera o valor opaco entregue ao cliente como refresh token. */
public interface RefreshTokenGenerator {

    String generate();
}
