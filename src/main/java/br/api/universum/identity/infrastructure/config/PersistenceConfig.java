package br.api.universum.identity.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Fills {@code created_at} / {@code updated_at} of the JPA entities. */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {
}
