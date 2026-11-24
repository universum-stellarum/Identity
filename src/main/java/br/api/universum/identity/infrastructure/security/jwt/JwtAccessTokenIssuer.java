package br.api.universum.identity.infrastructure.security.jwt;

import br.api.universum.identity.application.port.AccessTokenIssuer;
import br.api.universum.identity.domain.model.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    public static final long EXPIRES_IN_SECONDS = 900;

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final String audience;

    public JwtAccessTokenIssuer(JwtEncoder jwtEncoder,
                                Clock clock,
                                @Value("${app.jwt.issuer}") String issuer,
                                @Value("${app.jwt.audience}") String audience) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    public AccessToken issue(User user) {
        Instant now = clock.instant();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(EXPIRES_IN_SECONDS))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("scope", user.getRoles())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new AccessToken(token, EXPIRES_IN_SECONDS);
    }
}
