package br.api.universum.identity.application.port;

import br.api.universum.identity.domain.event.DomainEvent;

public interface DomainEventPublisherPort {

    /** Os listeners só são notificados depois do commit da unidade de trabalho atual. */
    void publish(DomainEvent event);
}
