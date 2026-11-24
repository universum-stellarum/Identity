package br.api.universum.identity.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.time.Clock;

/**
 * Registra a camada de aplicação como beans. Casos de uso e serviços de aplicação não têm anotação
 * do Spring (só Lombok, que atua na compilação): são encontrados aqui pelo pacote e construídos pelo
 * único construtor, recebendo as implementações dos seus ports.
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
