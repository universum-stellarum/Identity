package br.api.universum.identity.infrastructure.endpoints;

import br.api.universum.identity.application.usecases.auth.LoginUseCase;
import br.api.universum.identity.application.usecases.auth.LogoutUseCase;
import br.api.universum.identity.application.usecases.auth.RefreshSessionUseCase;
import br.api.universum.identity.application.usecases.keys.RotateSigningKeyUseCase;
import br.api.universum.identity.application.usecases.auth.LoginRequest;
import br.api.universum.identity.application.usecases.auth.RefreshTokenRequest;
import br.api.universum.identity.application.usecases.keys.SigningKeyResponse;
import br.api.universum.identity.application.usecases.auth.TokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identity/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login, renovação e logout dos tokens (JWT)")
public class AuthController {

    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;
    private final RotateSigningKeyUseCase rotateSigningKey;

    @PostMapping("/keys")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gera uma nova chave de assinatura dos JWT",
            description = "Exige autenticação. Substitui a chave ativa e invalida os access tokens existentes. "
                    + "Só os metadados da chave são retornados; os refresh tokens continuam válidos.")
    public SigningKeyResponse generateKeys() {
        return rotateSigningKey.execute();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Faz logout",
            description = "Revoga o refresh token informado. Idempotente; o access token expira sozinho.")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.execute(request);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Faz login",
            description = "Autentica e-mail e senha e emite um par de tokens (access token JWT + refresh token)"
    )
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return login.execute(request);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renova o access token",
            description = "Valida o refresh token informado, revoga-o (rotação) e emite novos tokens"
    )
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return refreshSession.execute(request);
    }
}
