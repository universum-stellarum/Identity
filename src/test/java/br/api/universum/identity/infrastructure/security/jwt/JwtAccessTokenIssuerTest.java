package br.api.universum.identity.infrastructure.security.jwt;

import br.api.universum.identity.application.port.AccessTokenIssuer.AccessToken;
import br.api.universum.identity.domain.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAccessTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private JwtEncoder jwtEncoder;

    private User user(UUID id) {
        return User.restore(id, "john@universum.api.br", "encoded", "John", User.DEFAULT_ROLE, 0, null);
    }

    private JwtClaimsSet capturedClaims() {
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        return captor.getValue().getClaims();
    }

    @Test
    void shouldEncodeUserClaimsAndExpireInFifteenMinutes() {
        UUID userId = UUID.randomUUID();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(
                Jwt.withTokenValue("signed-jwt").header("alg", "RS256").subject(userId.toString()).build());
        var issuer = new JwtAccessTokenIssuer(jwtEncoder, CLOCK, "https://production.universum.api.br", "universum-api");

        AccessToken token = issuer.issue(user(userId));

        assertThat(token.value()).isEqualTo("signed-jwt");
        assertThat(token.expiresInSeconds()).isEqualTo(900L);

        JwtClaimsSet claims = capturedClaims();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer().toString()).isEqualTo("https://production.universum.api.br");
        assertThat(claims.getAudience()).containsExactly("universum-api");
        assertThat(claims.<String>getClaim("email")).isEqualTo("john@universum.api.br");
        assertThat(claims.<String>getClaim("name")).isEqualTo("John");
        assertThat(claims.<String>getClaim("scope")).isEqualTo(User.DEFAULT_ROLE);
        assertThat(claims.getIssuedAt()).isEqualTo(NOW);
        assertThat(Duration.between(claims.getIssuedAt(), claims.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void usesTheIssuerConfiguredForTheDeployment() {
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(
                Jwt.withTokenValue("jwt").header("alg", "RS256").claim("sub", "user").build());
        var issuer = new JwtAccessTokenIssuer(jwtEncoder, CLOCK, "https://development.universum.api.br", "universum-api");

        issuer.issue(user(UUID.randomUUID()));

        assertThat(capturedClaims().getIssuer().toString()).isEqualTo("https://development.universum.api.br");
    }
}
