package br.api.universum.identity.support;

import br.api.universum.identity.infrastructure.security.Sha256TokenHasher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class H2PostgresFunctionsTest {

    /** Migration V3 must produce the same hash the application computes for a refresh token. */
    @Test
    void reproducesTheHashOfMigrationV3() throws Exception {
        String token = "3f1c2a9e-legacy-refresh-token";

        String migrated = H2PostgresFunctions.encode(
                H2PostgresFunctions.sha256(H2PostgresFunctions.convertTo(token, "UTF8")), "hex");

        assertThat(migrated).isEqualTo(new Sha256TokenHasher().hash(token));
    }
}
