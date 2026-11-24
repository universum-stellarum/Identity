package br.api.universum.identity.infrastructure.security.jwt;

import br.api.universum.identity.application.port.SigningKeyGenerator;

import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

@Component
public class RsaSigningKeyGenerator implements SigningKeyGenerator {

    private static final int KEY_SIZE = 2048;

    @Override
    public GeneratedKeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            KeyPair pair = generator.generateKeyPair();
            return new GeneratedKeyPair(
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to generate JWT signing key", e);
        }
    }
}
