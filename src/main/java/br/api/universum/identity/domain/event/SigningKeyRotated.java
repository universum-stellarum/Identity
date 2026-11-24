package br.api.universum.identity.domain.event;

import java.util.UUID;

/** The active signing key was replaced. Access tokens signed with the previous key are no longer valid. */
public record SigningKeyRotated(UUID keyId) implements DomainEvent {
}
