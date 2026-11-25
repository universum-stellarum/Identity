package br.api.universum.identity.infrastructure.security.jwt;

import br.api.universum.identity.application.usecases.keys.GetActiveSigningKeyUseCase;
import br.api.universum.identity.application.usecases.keys.InitializeSigningKeyUseCase;
import br.api.universum.identity.domain.event.SigningKeyRotated;
import br.api.universum.identity.domain.model.SigningKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monta o {@link JwtEncoder} e o {@link JwtDecoder} a partir da chave gravada no banco e os mantém em
 * cache por alguns segundos. As outras instâncias pegam uma chave rotacionada quando o cache delas expira.
 */
@Component
public class JwtKeyProvider {

    private static final Duration CACHE_TTL = Duration.ofSeconds(5);

    private final GetActiveSigningKeyUseCase activeKey;
    private final Clock clock;
    private final JwtKeyMaterialFactory materialFactory;
    private final AtomicReference<CachedKey> cached = new AtomicReference<>();

    @Autowired
    public JwtKeyProvider(InitializeSigningKeyUseCase initializeKey,
                          GetActiveSigningKeyUseCase activeKey,
                          Clock clock,
                          @Value("${app.jwt.issuer}") String issuer) {
        this.activeKey = activeKey;
        this.clock = clock;
        this.materialFactory = new JwtKeyMaterialFactory(issuer);
        this.cached.set(load(initializeKey.execute(), clock.instant().plus(CACHE_TTL)));
    }

    /** Encoder que assina com a chave ativa e coloca o {@code kid} dela no header do token. */
    public JwtEncoder jwtEncoder() {
        return parameters -> {
            CachedKey key = current();
            JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.id().toString()).build();
            return key.encoder().encode(JwtEncoderParameters.from(header, parameters.getClaims()));
        };
    }

    /** Decoder que valida tokens com a chave ativa. */
    public JwtDecoder jwtDecoder() {
        return token -> current().decoder().decode(token);
    }

    /** Descarta o cache desta instância assim que uma rotação é confirmada. */
    @TransactionalEventListener
    public synchronized void onKeyRotated(SigningKeyRotated event) {
        // Executado após o commit: nunca guardar em cache a chave de uma rotação desfeita (rollback).
        cached.set(null);
    }

    private CachedKey current() {
        CachedKey key = cached.get();
        if (key != null && clock.instant().isBefore(key.expiresAt())) {
            return key;
        }
        return refresh();
    }

    private synchronized CachedKey refresh() {
        Instant now = clock.instant();
        CachedKey key = cached.get();
        if (key != null && now.isBefore(key.expiresAt())) {
            return key;
        }
        SigningKey stored = activeKey.execute();
        CachedKey refreshed = key != null && key.id().equals(stored.getKeyId())
                ? new CachedKey(key.id(), key.encoder(), key.decoder(), now.plus(CACHE_TTL))
                : load(stored, now.plus(CACHE_TTL));
        cached.set(refreshed);
        return refreshed;
    }

    private CachedKey load(SigningKey stored, Instant expiresAt) {
        JwtKeyMaterial material = materialFactory.create(stored);
        return new CachedKey(material.id(), material.encoder(), material.decoder(), expiresAt);
    }

    private record CachedKey(UUID id, JwtEncoder encoder, JwtDecoder decoder, Instant expiresAt) {
    }
}
