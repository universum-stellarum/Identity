package br.api.universum.identity.application.dto;

public record UpdateUserCommand(String email, String name, String roles) {
}
