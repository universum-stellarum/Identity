package br.api.universum.identity.infrastructure.endpoints;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Publica a chave pública de verificação dos tokens no formato JWK Set (RFC 7517). */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RSAKey signingKey;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> publicKeys() {
        // toPublicJWK() descarta a metade privada: só n, e e os metadados saem na resposta.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new JWKSet(signingKey.toPublicJWK()).toJSONObject());
    }
}
