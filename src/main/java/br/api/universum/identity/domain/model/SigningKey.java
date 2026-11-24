package br.api.universum.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import static br.api.universum.identity.domain.model.Guard.requireNonNull;
import static br.api.universum.identity.domain.model.Guard.requireText;

/**
 * The RSA key pair that signs access tokens. There is a single active key: rotating it
 * replaces the pair and invalidates every access token signed with the previous one.
 *
 * <p>Keys are kept encoded in Base64: the public key as X.509 and the private key as PKCS#8.</p>
 *
 * <p>Stored in a single row, created by migration V2 with every key column empty, so that
 * {@code SELECT ... FOR UPDATE} can serialize the first key generation across instances.</p>
 */
@Getter
@Entity
@Table(name = "jwt_signing_key")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SigningKey extends AuditableEntity {

    public static final String ALGORITHM = "RS256";
    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "key_id")
    private UUID keyId;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    /** Rebuilds the key in a known state. All key fields are {@code null} while no pair was generated. */
    public static SigningKey restore(UUID keyId, String publicKey, String privateKey, Instant rotatedAt) {
        return new SigningKey(SINGLETON_ID, keyId, publicKey, privateKey, rotatedAt);
    }

    public boolean isInitialized() {
        return keyId != null;
    }

    /** Replaces the whole pair at once; on invalid input nothing changes. */
    public void rotate(UUID keyId, String publicKey, String privateKey, Instant rotatedAt) {
        requireNonNull(keyId, "Key id is required.");
        requireText(publicKey, "Public key is required.");
        requireText(privateKey, "Private key is required.");
        requireNonNull(rotatedAt, "Rotation instant is required.");

        this.keyId = keyId;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.rotatedAt = rotatedAt;
    }
}
