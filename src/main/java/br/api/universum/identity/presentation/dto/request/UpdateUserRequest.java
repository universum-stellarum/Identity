package br.api.universum.identity.presentation.dto.request;

import br.api.universum.identity.application.dto.UpdateUserCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email.")
        @Size(max = 100, message = "Email must be at most 100 characters.")
        String email,

        @NotBlank(message = "Name is required.")
        @Size(max = 100, message = "Name must be at most 100 characters.")
        String name,

        @NotBlank(message = "Roles are required.")
        String roles) {

    public UpdateUserCommand toCommand() {
        return new UpdateUserCommand(email, name, roles);
    }
}
