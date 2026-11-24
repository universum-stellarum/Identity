package br.api.universum.identity.integration;

import br.api.universum.identity.application.dto.LoginCommand;
import br.api.universum.identity.application.port.SigningKeyRepository;
import br.api.universum.identity.application.port.UserRepository;
import br.api.universum.identity.application.usecase.InitializeSigningKeyUseCase;
import br.api.universum.identity.application.usecase.LoginUseCase;
import br.api.universum.identity.application.usecase.RefreshSessionUseCase;
import br.api.universum.identity.application.usecase.RotateSigningKeyUseCase;
import br.api.universum.identity.domain.model.SigningKey;
import br.api.universum.identity.domain.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class JwtKeyIntegrationTest {

    @Autowired InitializeSigningKeyUseCase initializeKey;
    @Autowired RotateSigningKeyUseCase rotateKey;
    @Autowired SigningKeyRepository keys;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired LoginUseCase login;
    @Autowired RefreshSessionUseCase refreshSession;
    @Autowired PasswordEncoder passwords;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JwtDecoder decoder;

    private SigningKey activeKey() {
        return keys.findActive().orElseThrow();
    }

    private int keyRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM jwt_signing_key", Integer.class);
    }

    @Test
    void startupGeneratesOnePersistentPairAndDoesNotReplaceItOnInitialization() {
        SigningKey first = activeKey();
        assertThat(first.getKeyId()).isNotNull();
        assertThat(first.getPrivateKey()).isNotBlank();
        assertThat(initializeKey.execute().getKeyId()).isEqualTo(first.getKeyId());
        assertThat(keyRows()).isEqualTo(1);
    }

    @Test
    void concurrentInitializationsReuseTheSameKey() throws Exception {
        jdbc.update("UPDATE jwt_signing_key SET key_id = NULL, public_key = NULL, private_key = NULL, rotated_at = NULL WHERE id = 1");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<UUID> initialize = () -> {
                start.await();
                return initializeKey.execute().getKeyId();
            };
            var first = executor.submit(initialize);
            var second = executor.submit(initialize);
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(second.get(10, TimeUnit.SECONDS));
        }
        assertThat(keyRows()).isEqualTo(1);
    }

    @Test
    void rotationRequiresAuthenticationAndNeverReturnsPrivateMaterial() throws Exception {
        mvc.perform(post("/identity/api/v1/auth/keys")).andExpect(status().isUnauthorized());
        var user = users.save(User.register("keys@universum.api.br", passwords.encode("secret123"), "Keys"));
        var tokens = login.execute(new LoginCommand(user.getEmail(), "secret123"));
        UUID oldId = activeKey().getKeyId();
        mvc.perform(post("/identity/api/v1/auth/keys").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keyId").isNotEmpty())
                .andExpect(jsonPath("$.algorithm").value("RS256"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.publicKey").doesNotExist());
        assertThat(activeKey().getKeyId()).isNotEqualTo(oldId);
        mvc.perform(get("/identity/api/v1/users/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isUnauthorized());
        var renewed = refreshSession.execute(tokens.refreshToken());
        mvc.perform(get("/identity/api/v1/users/me").header("Authorization", "Bearer " + renewed.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void migrationAllowsOnlyOneSigningKeyRow() {
        var previous = activeKey().getKeyId();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO jwt_signing_key (id) VALUES (2)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(activeKey().getKeyId()).isEqualTo(previous);
    }

    @Test
    void rolledBackRotationPreservesThePersistedAndCachedKey() {
        users.save(User.register("rollback@universum.api.br", passwords.encode("secret123"), "Rollback"));
        var tokens = login.execute(new LoginCommand("rollback@universum.api.br", "secret123"));
        var previous = activeKey().getKeyId();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            rotateKey.execute();
            status.setRollbackOnly();
        });
        assertThat(activeKey().getKeyId()).isEqualTo(previous);
        assertThat(decoder.decode(tokens.accessToken()).getSubject()).isNotBlank();
    }
}
