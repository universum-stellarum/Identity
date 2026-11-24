package br.api.universum.identity.infrastructure.event;

import br.api.universum.identity.application.port.DomainEventPublisher;
import br.api.universum.identity.domain.event.DomainEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events as Spring application events. Listeners use
 * {@code @TransactionalEventListener}, so they only run after the unit of work commits.
 */
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher events;

    @Override
    public void publish(DomainEvent event) {
        events.publishEvent(event);
    }
}
