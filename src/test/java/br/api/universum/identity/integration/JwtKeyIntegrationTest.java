package br.api.universum.identity.integration;

import br.api.universum.identity.application.abstractions.SigningKeyRepository;
import br.api.universum.identity.application.usecases.keys.InitializeSigningKeyUseCase;
import br.api.universum.identity.domain.model.SigningKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class JwtKeyIntegrationTest {

    @Autowired InitializeSigningKeyUseCase initializeKey;
    @Autowired SigningKeyRepository keys;
    @Autowired JdbcTemplate jdbc;

    private SigningKey storedKey() {
        return keys.find().filter(SigningKey::isInitialized).orElseThrow();
    }

    private int keyRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM jwt_signing_key", Integer.class);
    }

    @Test
    void startupGeneratesOnePersistentPairAndDoesNotReplaceItOnInitialization() {
        SigningKey first = storedKey();
        assertThat(first.getKeyId()).isNotNull();
        assertThat(first.getPrivateKey()).isNotBlank();
        assertThat(initializeKey.execute().getKeyId()).isEqualTo(first.getKeyId());
        assertThat(keyRows()).isEqualTo(1);
    }

    @Test
    void migrationAllowsOnlyOneSigningKeyRow() {
        var previous = storedKey().getKeyId();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO jwt_signing_key (id) VALUES (2)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(storedKey().getKeyId()).isEqualTo(previous);
    }
}
