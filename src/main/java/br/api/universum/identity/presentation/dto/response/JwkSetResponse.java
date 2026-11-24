package br.api.universum.identity.presentation.dto.response;

import br.api.universum.identity.application.dto.VerificationKey;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.experimental.UtilityClass;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/** Builds the JWK Set document (RFC 7517) that publishes the verification key. */
@UtilityClass
public class JwkSetResponse {

    public Map<String, Object> from(VerificationKey key) {
        try {
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(key.publicKey())));
            RSAKey jwk = new RSAKey.Builder(publicKey)
                    .keyID(key.keyId().toString())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.parse(key.algorithm()))
                    .build();
            return new JWKSet(jwk).toJSONObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to publish JWT verification key", exception);
        }
    }
}
