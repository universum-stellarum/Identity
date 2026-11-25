package br.api.universum.identity.infrastructure.security.jwt;

import br.api.universum.identity.application.usecases.keys.InitializeSigningKeyUseCase;
import br.api.universum.identity.domain.model.SigningKey;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Carrega a chave de assinatura uma vez, na subida (gerando-a se ainda não existir), e monta a partir
 * dela o encoder que assina os tokens e o decoder que os valida.
 */
@Configuration
public class JwtConfig {

    /** A chave no formato JWK, com as duas metades. A pública é publicada pelo JWKS. */
    @Bean
    public RSAKey signingKey(InitializeSigningKeyUseCase initializeKey) {
        SigningKey stored = initializeKey.execute();
        try {
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) rsa.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(stored.getPublicKey())));
            RSAPrivateKey privateKey = (RSAPrivateKey) rsa.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(stored.getPrivateKey())));

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(stored.getKeyId().toString())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.parse(SigningKey.ALGORITHM))
                    .build();
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to load JWT signing key from the database", e);
        }
    }

    /** Assina com a chave privada e coloca o {@code kid} da chave no header do token. */
    @Bean
    public JwtEncoder jwtEncoder(RSAKey signingKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey)));
    }

    /** Valida assinatura, expiração e emissor ({@code iss}) dos tokens recebidos pelo identity. */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey signingKey, @Value("${app.jwt.issuer}") String issuer) {
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(signingKey.toRSAPublicKey()).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
            return decoder;
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to read the JWT public key", e);
        }
    }
}
