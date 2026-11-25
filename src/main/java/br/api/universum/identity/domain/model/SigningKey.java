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
 * O par de chaves RSA que assina os access tokens. É gerado uma única vez, na primeira subida do
 * serviço, e nunca é substituído.
 *
 * <p>As chaves ficam codificadas em Base64: a pública em X.509 e a privada em PKCS#8.</p>
 *
 * <p>Fica numa única linha, criada vazia pela migration V2 e preenchida na primeira subida.</p>
 */
@Getter
@Entity
@Table(name = "jwt_signing_key")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SigningKey extends BaseEntity<Integer> {

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

    /** Reconstrói a chave num estado conhecido. Os campos da chave ficam {@code null} enquanto nenhum par foi gerado. */
    public static SigningKey restore(
            UUID keyId,
            String publicKey,
            String privateKey,
            Instant rotatedAt
    ) {
        return new SigningKey(
                SINGLETON_ID,
                keyId,
                publicKey,
                privateKey,
                rotatedAt
        );
    }

    /** {@code false} enquanto o primeiro par de chaves ainda não foi gerado. */
    public boolean isInitialized() {
        return keyId != null;
    }

    /**
     * Grava o primeiro par de chaves, todo de uma vez. Com entrada inválida, nada muda.
     *
     * @throws IllegalStateException se a chave já foi gerada: ela nunca é substituída
     */
    public void initialize(UUID keyId, String publicKey, String privateKey, Instant rotatedAt) {
        if (isInitialized()) {
            throw new IllegalStateException("JWT signing key is already initialized.");
        }
        requireNonNull(keyId, "Key id is required.");
        requireText(publicKey, "Public key is required.");
        requireText(privateKey, "Private key is required.");
        requireNonNull(rotatedAt, "Creation instant is required.");

        this.keyId = keyId;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.rotatedAt = rotatedAt;
    }
}
