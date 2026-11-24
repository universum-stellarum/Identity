package br.api.universum.identity.application.dto;

public record RegisterUserCommand(String email, String password, String name) {
}
