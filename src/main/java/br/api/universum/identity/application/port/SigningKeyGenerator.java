package br.api.universum.identity.application.port;

/** Generates a new RSA key pair for signing access tokens. */
public interface SigningKeyGenerator {

    GeneratedKeyPair generate();

    /**
     * @param publicKey  Base64 of the X.509 encoded public key
     * @param privateKey Base64 of the PKCS#8 encoded private key
     */
    record GeneratedKeyPair(String publicKey, String privateKey) {
    }
}
