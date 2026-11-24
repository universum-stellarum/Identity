package br.api.universum.identity.presentation.controller;

import br.api.universum.identity.application.usecase.LoginUseCase;
import br.api.universum.identity.application.usecase.LogoutUseCase;
import br.api.universum.identity.application.usecase.RefreshSessionUseCase;
import br.api.universum.identity.application.usecase.RotateSigningKeyUseCase;
import br.api.universum.identity.presentation.dto.request.LoginRequest;
import br.api.universum.identity.presentation.dto.request.RefreshTokenRequest;
import br.api.universum.identity.presentation.dto.response.JwtKeyResponse;
import br.api.universum.identity.presentation.dto.response.TokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    public JwtKeyResponse generateKeys() {
        return JwtKeyResponse.from(rotateSigningKey.execute());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Faz logout",
            description = "Revoga o refresh token informado. Idempotente; o access token expira sozinho.")
    @ApiResponse(responseCode = "204", description = "Logout realizado")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.execute(request.refreshToken());
    }

    @PostMapping("/login")
    @Operation(
            summary = "Faz login",
            description = "Autentica e-mail e senha e emite um par de tokens (access token JWT + refresh token)"
    )
    @ApiResponse(responseCode = "200", description = "Login realizado")
    @ApiResponse(responseCode = "400", description = "Credenciais inválidas ou conta temporariamente bloqueada")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(login.execute(request.toCommand()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renova o access token",
            description = "Valida o refresh token informado, revoga-o (rotação) e emite novos tokens"
    )
    @ApiResponse(responseCode = "200", description = "Tokens renovados")
    @ApiResponse(responseCode = "400", description = "Refresh token inválido, expirado ou já revogado")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(refreshSession.execute(request.refreshToken()));
    }
}
