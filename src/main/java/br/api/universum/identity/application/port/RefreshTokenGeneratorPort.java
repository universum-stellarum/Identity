package br.api.universum.identity.application.port;

/** Gera o valor opaco entregue ao cliente como refresh token. */
public interface RefreshTokenGeneratorPort {

    String generate();
}
