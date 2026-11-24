package br.api.universum.identity.application.port;

/** One-way hash used to store and look up refresh tokens without persisting their value. */
public interface TokenHasher {

    String hash(String rawToken);
}
