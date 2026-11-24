package br.api.universum.identity.presentation.dto.request;

import br.api.universum.identity.application.dto.LoginCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email.")
        String email,

        @NotBlank(message = "Password is required.")
        String password) {

    public LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }
}
