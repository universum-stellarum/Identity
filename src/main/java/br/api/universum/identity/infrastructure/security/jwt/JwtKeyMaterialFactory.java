package br.api.universum.identity.infrastructure.security.jwt;

import br.api.universum.identity.domain.model.SigningKey;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class JwtKeyMaterialFactory {

    private final String issuer;

    JwtKeyMaterial create(SigningKey stored) {
        try {
            RSAPublicKey publicKey = RsaKeys.publicKey(stored.getPublicKey());
            RSAPrivateKey privateKey = RsaKeys.privateKey(stored.getPrivateKey());
            validateKeyPair(publicKey, privateKey);

            RSAKey jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(stored.getKeyId().toString()).build();
            JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

            return new JwtKeyMaterial(stored.getKeyId(), encoder, decoder);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load JWT signing key from the database", e);
        }
    }

    private void validateKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey)
            throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        byte[] challenge = new byte[]{1, 2, 3};
        signature.initSign(privateKey);
        signature.update(challenge);
        byte[] signed = signature.sign();
        signature.initVerify(publicKey);
        signature.update(challenge);
        if (!signature.verify(signed)) {
            throw new IllegalStateException("JWT keys in the database do not match.");
        }
    }
}
