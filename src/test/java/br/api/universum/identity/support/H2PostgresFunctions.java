package br.api.universum.identity.support;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * PostgreSQL functions used by the Flyway migrations, registered in H2 (see the datasource URL in
 * {@code application-test.yaml}). The migrations themselves cannot change: they were already applied
 * in dev and prod, and Flyway validates their checksums.
 */
public final class H2PostgresFunctions {

    private H2PostgresFunctions() {
    }

    /** {@code convert_to(text, encoding)}: the text encoded as bytes. */
    public static byte[] convertTo(String text, String encoding) {
        return text == null ? null : text.getBytes(Charset.forName(encoding));
    }

    /** {@code sha256(bytea)}. */
    public static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
        return data == null ? null : MessageDigest.getInstance("SHA-256").digest(data);
    }

    /** {@code encode(bytea, 'hex')}: lowercase hex, the only format the migrations use. */
    public static String encode(byte[] data, String format) {
        if (data == null) {
            return null;
        }
        if (!"hex".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Unsupported encode format: " + format);
        }
        return HexFormat.of().formatHex(data);
    }
}
