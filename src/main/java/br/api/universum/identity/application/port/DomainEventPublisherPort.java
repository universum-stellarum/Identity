package br.api.universum.identity.application.port;

import br.api.universum.identity.domain.event.DomainEvent;

public interface DomainEventPublisherPort {

    /** Listeners are notified only after the current unit of work commits. */
    void publish(DomainEvent event);
}
