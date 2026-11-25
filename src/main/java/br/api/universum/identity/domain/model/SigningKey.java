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
 * O par de chaves RSA que assina os access tokens. Existe uma única chave ativa: rotacioná-la
 * substitui o par e invalida os access tokens assinados com o anterior.
 *
 * <p>As chaves ficam codificadas em Base64: a pública em X.509 e a privada em PKCS#8.</p>
 *
 * <p>Fica numa única linha, criada pela migration V2 com todas as colunas da chave vazias, para que o
 * {@code SELECT ... FOR UPDATE} consiga serializar a primeira geração da chave entre as instâncias.</p>
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

    /** Substitui o par inteiro de uma vez; com entrada inválida, nada muda. */
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
