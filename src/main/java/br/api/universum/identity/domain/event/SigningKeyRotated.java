package br.api.universum.identity.domain.event;

import java.util.UUID;

/** A chave de assinatura ativa foi substituída. Access tokens assinados com a anterior deixam de valer. */
public record SigningKeyRotated(UUID keyId) {
}
