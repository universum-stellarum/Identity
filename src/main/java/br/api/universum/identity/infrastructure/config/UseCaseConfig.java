package br.api.universum.identity.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.time.Clock;

/**
 * Registers the application layer as beans. Use cases and application services have no Spring
 * annotation (only Lombok, which acts at compile time): they are picked up here by package and
 * constructed through their single constructor, receiving the adapters that implement their ports.
 */
@Configuration
@ComponentScan(
        basePackages = "br.api.universum.identity.application",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "br\\.api\\.universum\\.identity\\.application\\.(usecase|service)\\..*"
        )
)
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
