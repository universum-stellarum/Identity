package br.api.universum.identity.application.abstractions;

/** Hash de mão única usado para guardar e localizar refresh tokens sem persistir o valor. */
public interface TokenHasher {

    String hash(String rawToken);
}
