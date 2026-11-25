package br.api.universum.identity.application.usecases.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo do login: as credenciais do usuário. */
public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email.")
        String email,

        @NotBlank(message = "Password is required.")
        String password) {
}
