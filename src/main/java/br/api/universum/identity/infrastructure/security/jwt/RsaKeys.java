package br.api.universum.identity.infrastructure.security.jwt;

import lombok.experimental.UtilityClass;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Decodifica os formatos de chave em Base64 gravados no banco. */
@UtilityClass
class RsaKeys {

    RSAPublicKey publicKey(String base64X509) throws GeneralSecurityException {
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64X509)));
    }

    RSAPrivateKey privateKey(String base64Pkcs8) throws GeneralSecurityException {
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Pkcs8)));
    }
}
