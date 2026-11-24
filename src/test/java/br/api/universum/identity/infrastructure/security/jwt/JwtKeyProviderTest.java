package br.api.universum.identity.infrastructure.security.jwt;

import br.api.universum.identity.application.usecases.keys.GetActiveSigningKeyUseCase;
import br.api.universum.identity.application.usecases.keys.InitializeSigningKeyUseCase;
import br.api.universum.identity.domain.event.SigningKeyRotated;
import br.api.universum.identity.domain.model.SigningKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtKeyProviderTest {

    private static final String PRODUCTION = "https://production.universum.api.br";
    private static final String DEVELOPMENT = "https://development.universum.api.br";

    private final InitializeSigningKeyUseCase initialize = mock(InitializeSigningKeyUseCase.class);
    private final GetActiveSigningKeyUseCase activeKey = mock(GetActiveSigningKeyUseCase.class);
    private final Clock clock = mock(Clock.class);
    private final Instant now = Instant.now();
    private SigningKey key;

    @BeforeEach
    void setUp() {
        key = key();
        when(clock.instant()).thenReturn(now);
        when(initialize.execute()).thenReturn(key);
    }

    private SigningKey key() {
        var pair = new RsaSigningKeyGenerator().generate();
        return SigningKey.restore(UUID.randomUUID(), pair.publicKey(), pair.privateKey(), now);
    }

    private JwtKeyProvider provider(String issuer) {
        return new JwtKeyProvider(initialize, activeKey, clock, issuer);
    }

    private String token(JwtKeyProvider provider) {
        var claims = JwtClaimsSet.builder().issuer(PRODUCTION).subject("user")
                .issuedAt(now).expiresAt(now.plusSeconds(900)).build();
        return provider.jwtEncoder().encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Test
    void reusesPersistedKeysAcrossInstancesAndCachesThem() {
        var first = provider(PRODUCTION);
        var restarted = provider(PRODUCTION);
        String token = token(first);
        assertThat(restarted.jwtDecoder().decode(token).getSubject()).isEqualTo("user");
        assertThat(restarted.jwtDecoder().decode(token).getHeaders()).containsEntry("kid", key.getKeyId().toString());
        verify(activeKey, never()).execute();
    }

    @Test
    void updatesOtherInstancesAfterCacheExpiry() {
        var first = provider(PRODUCTION);
        var replica = provider(PRODUCTION);
        String oldToken = token(first);
        SigningKey replacement = key();
        when(activeKey.execute()).thenReturn(replacement);
        first.onKeyRotated(new SigningKeyRotated(replacement.getKeyId()));
        String newToken = token(first);
        var firstDecoder = first.jwtDecoder();
        assertThatThrownBy(() -> firstDecoder.decode(oldToken)).isInstanceOf(JwtException.class);
        when(clock.instant()).thenReturn(now.plusSeconds(5));
        assertThat(replica.jwtDecoder().decode(newToken).getHeaders())
                .containsEntry("kid", replacement.getKeyId().toString());
        var replicaDecoder = replica.jwtDecoder();
        assertThatThrownBy(() -> replicaDecoder.decode(oldToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void reloadsOnlyOncePerCacheIntervalWhenTheKeyHasNotChanged() {
        var provider = provider(PRODUCTION);
        String token = token(provider);
        when(activeKey.execute()).thenReturn(key);
        when(clock.instant()).thenReturn(now.plusSeconds(5));
        provider.jwtDecoder().decode(token);
        provider.jwtDecoder().decode(token);
        token(provider);
        verify(activeKey, times(1)).execute();
    }

    @Test
    void rejectsMismatchedKeysStoredInTheDatabase() {
        SigningKey other = key();
        key.rotate(key.getKeyId(), key.getPublicKey(), other.getPrivateKey(), now);
        assertThatThrownBy(() -> provider(PRODUCTION))
                .hasRootCauseMessage("JWT keys in the database do not match.");
    }

    @Test
    void concurrentRequestsRefreshExpiredCacheOnlyOnce() throws Exception {
        var provider = provider(PRODUCTION);
        String token = token(provider);
        var decoder = provider.jwtDecoder();
        when(activeKey.execute()).thenReturn(key);
        when(clock.instant()).thenReturn(now.plusSeconds(5));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var results = new ArrayList<Future<String>>();
            for (int i = 0; i < 16; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return decoder.decode(token).getSubject();
                }));
            }
            start.countDown();
            for (var result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).isEqualTo("user");
            }
        }
        verify(activeKey, times(1)).execute();
    }

    @Test
    void developmentAndProductionRejectEachOthersTokensEvenWithTheSameKey() {
        var development = provider(DEVELOPMENT);
        var production = provider(PRODUCTION);
        for (String issuer : new String[]{DEVELOPMENT, PRODUCTION}) {
            var claims = JwtClaimsSet.builder().issuer(issuer).subject("user")
                    .issuedAt(now).expiresAt(now.plusSeconds(900)).build();
            String token = development.jwtEncoder().encode(JwtEncoderParameters.from(claims)).getTokenValue();
            var expected = issuer.equals(DEVELOPMENT) ? development.jwtDecoder() : production.jwtDecoder();
            var other = issuer.equals(DEVELOPMENT) ? production.jwtDecoder() : development.jwtDecoder();
            assertThat(expected.decode(token).getSubject()).isEqualTo("user");
            assertThatThrownBy(() -> other.decode(token)).isInstanceOf(JwtException.class);
        }
    }
}
