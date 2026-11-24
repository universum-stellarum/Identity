package br.api.universum.identity.application.abstractions;

/** Gera um novo par de chaves RSA para assinar os access tokens. */
public interface SigningKeyGenerator {

    GeneratedKeyPair generate();

    /**
     * @param publicKey  Base64 da chave pública codificada em X.509
     * @param privateKey Base64 da chave privada codificada em PKCS#8
     */
    record GeneratedKeyPair(String publicKey, String privateKey) {
    }
}
