package br.api.universum.identity.application.port;

/** Hash de mão única usado para guardar e localizar refresh tokens sem persistir o valor. */
public interface TokenHasherPort {

    String hash(String rawToken);
}
