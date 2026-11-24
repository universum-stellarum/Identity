package br.api.universum.identity.application.usecase;

import br.api.universum.identity.application.ImmediateUnitOfWork;
import br.api.universum.identity.application.port.RefreshTokenRepositoryPort;
import br.api.universum.identity.domain.model.RefreshToken;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock private RefreshTokenRepositoryPort refreshTokens;

    private LogoutUseCase logout() {
        return new LogoutUseCase(refreshTokens, raw -> "hash:" + raw, new ImmediateUnitOfWork());
    }

    @Test
    void shouldRevokeTheGivenToken() {
        RefreshToken token = RefreshToken.restore(UUID.randomUUID(), "hash:token", UUID.randomUUID(),
                Instant.now().plusSeconds(60), false);
        when(refreshTokens.findByTokenHashForUpdate("hash:token")).thenReturn(Optional.of(token));

        logout().execute("token");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokens).save(token);
    }

    @Test
    void shouldIgnoreUnknownTokens() {
        when(refreshTokens.findByTokenHashForUpdate("hash:unknown")).thenReturn(Optional.empty());

        assertThatCode(() -> logout().execute("unknown")).doesNotThrowAnyException();
        verify(refreshTokens, never()).save(any());
    }
}
