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
@Tag(name = "Authentication", description = "Endpoints for login and token (JWT) renewal")
public class AuthController {

    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;
    private final RotateSigningKeyUseCase rotateSigningKey;

    @PostMapping("/keys")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate a new JWT signing key",
            description = "Requires authentication. Replaces the active key and invalidates existing access tokens. "
                    + "Only key metadata is returned; refresh tokens remain usable.")
    public JwtKeyResponse generateKeys() {
        return JwtKeyResponse.from(rotateSigningKey.execute());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.execute(request.refreshToken());
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = "Authenticates email/password and issues a token pair (JWT access token + refresh token)"
    )
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "400", description = "Invalid credentials or account temporarily locked")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(login.execute(request.toCommand()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Validates the given refresh token, revokes it for rotation security, and issues new tokens"
    )
    @ApiResponse(responseCode = "200", description = "Refresh successful")
    @ApiResponse(responseCode = "400", description = "Invalid, expired, or already revoked refresh token")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(refreshSession.execute(request.refreshToken()));
    }
}
