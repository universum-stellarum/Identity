package br.api.universum.identity.infrastructure.security.jwt;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Decodes the Base64 key formats stored in the database. */
final class RsaKeys {

    private RsaKeys() {
    }

    static RSAPublicKey publicKey(String base64X509) throws GeneralSecurityException {
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64X509)));
    }

    static RSAPrivateKey privateKey(String base64Pkcs8) throws GeneralSecurityException {
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Pkcs8)));
    }
}
