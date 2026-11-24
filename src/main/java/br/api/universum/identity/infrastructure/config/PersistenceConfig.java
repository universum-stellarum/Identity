package br.api.universum.identity.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Preenche {@code created_at} / {@code updated_at} das entidades ({@code @CreatedDate}, {@code @LastModifiedDate}). */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {
}
