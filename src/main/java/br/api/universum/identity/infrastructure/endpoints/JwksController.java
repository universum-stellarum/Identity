package br.api.universum.identity.infrastructure.endpoints;

import br.api.universum.identity.application.usecases.keys.GetVerificationKeyUseCase;
import br.api.universum.identity.application.usecases.keys.VerificationKeyResponse;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/** Publica a chave de verificação dos tokens no formato JWK Set (RFC 7517). */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final GetVerificationKeyUseCase verificationKey;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> publicKeys() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(jwkSet(verificationKey.execute()));
    }

    private static Map<String, Object> jwkSet(VerificationKeyResponse key) {
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
