package br.api.universum.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

import static br.api.universum.identity.domain.model.Guard.requireNonNull;
import static br.api.universum.identity.domain.model.Guard.requireText;

/**
 * The RSA key pair that signs access tokens. There is a single active key: rotating it
 * replaces the pair and invalidates every access token signed with the previous one.
 *
 * <p>Keys are kept encoded in Base64: the public key as X.509 and the private key as PKCS#8.</p>
 */
public class SigningKey {

    public static final String ALGORITHM = "RS256";

    private UUID keyId;
    private String publicKey;
    private String privateKey;
    private Instant rotatedAt;

    private SigningKey(UUID keyId, String publicKey, String privateKey, Instant rotatedAt) {
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.rotatedAt = rotatedAt;
    }

    /**
     * Rebuilds the stored key. All fields are {@code null} while no pair was generated yet.
     */
    public static SigningKey restore(UUID keyId, String publicKey, String privateKey, Instant rotatedAt) {
        return new SigningKey(keyId, publicKey, privateKey, rotatedAt);
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

    public UUID getKeyId() {
        return keyId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }
}
