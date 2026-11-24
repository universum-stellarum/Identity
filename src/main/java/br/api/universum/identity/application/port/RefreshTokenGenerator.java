package br.api.universum.identity.application.port;

/** Generates the opaque value handed to the client as refresh token. */
public interface RefreshTokenGenerator {

    String generate();
}
