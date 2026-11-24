package br.api.universum.identity.presentation.dto.request;

import br.api.universum.identity.application.dto.UpdateProfileCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Email is required.") @Email(message = "Invalid email.")
        @Size(max = 100) String email,
        @NotBlank(message = "Name is required.") @Size(max = 100) String name) {

    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(email, name);
    }
}
