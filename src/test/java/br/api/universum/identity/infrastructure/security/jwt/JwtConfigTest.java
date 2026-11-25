package br.api.universum.identity.infrastructure.security.jwt;

import br.api.universum.identity.application.usecases.keys.InitializeSigningKeyUseCase;
import br.api.universum.identity.domain.model.SigningKey;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtConfigTest {

    private static final String PRODUCTION = "https://production.universum.api.br";
    private static final String DEVELOPMENT = "https://development.universum.api.br";

    private final JwtConfig config = new JwtConfig();
    private final Instant now = Instant.now();

    private RSAKey signingKey() {
        var pair = new RsaSigningKeyGenerator().generate();
        var stored = SigningKey.restore(UUID.randomUUID(), pair.publicKey(), pair.privateKey(), now);
        var initialize = mock(InitializeSigningKeyUseCase.class);
        when(initialize.execute()).thenReturn(stored);
        return config.signingKey(initialize);
    }

    private String token(JwtEncoder encoder, String issuer) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject("user")
                .issuedAt(now).expiresAt(now.plusSeconds(900)).build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Test
    void signsWithTheStoredKeyAndPublishesItsKid() {
        RSAKey key = signingKey();
        String token = token(config.jwtEncoder(key), PRODUCTION);

        var decoded = config.jwtDecoder(key, PRODUCTION).decode(token);

        assertThat(decoded.getSubject()).isEqualTo("user");
        assertThat(decoded.getHeaders())
                .containsEntry("kid", key.getKeyID())
                .containsEntry("alg", "RS256");
    }

    @Test
    void rejectsTokensSignedByAnotherKey() {
        String token = token(config.jwtEncoder(signingKey()), PRODUCTION);
        var decoder = config.jwtDecoder(signingKey(), PRODUCTION);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void developmentAndProductionRejectEachOthersTokensEvenWithTheSameKey() {
        RSAKey key = signingKey();
        var encoder = config.jwtEncoder(key);
        var production = config.jwtDecoder(key, PRODUCTION);
        String developmentToken = token(encoder, DEVELOPMENT);

        assertThat(config.jwtDecoder(key, DEVELOPMENT).decode(developmentToken).getSubject()).isEqualTo("user");
        assertThatThrownBy(() -> production.decode(developmentToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void failsFastWhenTheStoredKeyIsCorrupted() {
        var stored = SigningKey.restore(UUID.randomUUID(), "not-base64!", "not-base64!", now);
        var initialize = mock(InitializeSigningKeyUseCase.class);
        when(initialize.execute()).thenReturn(stored);

        assertThatThrownBy(() -> config.signingKey(initialize))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to load JWT signing key");
    }
}
