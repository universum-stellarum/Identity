package br.api.universum.identity.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenSecretsTest {

    @Test
    void refreshTokensAreUniqueUuids() {
        var generator = new UuidRefreshTokenGenerator();
        String first = generator.generate();
        String second = generator.generate();

        assertThat(UUID.fromString(first)).isNotNull();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashIsSha256InLowercaseHex() {
        // echo -n "abc" | sha256sum
        assertThat(new Sha256TokenHasher().hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
