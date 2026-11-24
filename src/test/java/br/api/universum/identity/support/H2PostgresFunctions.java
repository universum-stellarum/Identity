package br.api.universum.identity.support;

import lombok.experimental.UtilityClass;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Funções do PostgreSQL usadas pelas migrations do Flyway, registradas no H2 (veja a URL do datasource
 * em {@code application-test.yaml}). As migrations em si não podem mudar: já foram aplicadas em dev e
 * prod, e o Flyway valida o checksum delas.
 */
@UtilityClass
public class H2PostgresFunctions {

    /** {@code convert_to(text, encoding)}: o texto codificado em bytes. */
    public byte[] convertTo(String text, String encoding) {
        return text == null ? null : text.getBytes(Charset.forName(encoding));
    }

    /** {@code sha256(bytea)}. */
    public byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
        return data == null ? null : MessageDigest.getInstance("SHA-256").digest(data);
    }

    /** {@code encode(bytea, 'hex')}: hex em minúsculas, o único formato que as migrations usam. */
    public String encode(byte[] data, String format) {
        if (data == null) {
            return null;
        }
        if (!"hex".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Unsupported encode format: " + format);
        }
        return HexFormat.of().formatHex(data);
    }
}
