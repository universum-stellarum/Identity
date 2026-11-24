package br.api.universum.identity.infrastructure.event;

import br.api.universum.identity.application.port.DomainEventPublisherPort;
import br.api.universum.identity.domain.event.DomainEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publica os eventos de domínio como eventos de aplicação do Spring. Os listeners usam
 * {@code @TransactionalEventListener}, então só rodam depois do commit da unidade de trabalho.
 */
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisherPort {

    private final ApplicationEventPublisher events;

    @Override
    public void publish(DomainEvent event) {
        events.publishEvent(event);
    }
}
