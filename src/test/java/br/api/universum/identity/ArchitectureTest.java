package br.api.universum.identity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Garante a regra de dependência da arquitetura limpa: as dependências do código só apontam para dentro.
 *
 * <pre>
 * presentation ─┐
 *               ├─► application ─► domain
 * infrastructure┘
 * </pre>
 */
class ArchitectureTest {

    private static final String ROOT = "br.api.universum.identity";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void dependenciesPointInwards() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy(ROOT + ".domain..")
                .layer("Application").definedBy(ROOT + ".application..")
                .layer("Infrastructure").definedBy(ROOT + ".infrastructure..")
                .layer("Presentation").definedBy(ROOT + ".presentation..")
                .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Presentation")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Presentation")
                .check(CLASSES);
    }

    @Test
    void applicationDoesNotDependOnFrameworks() {
        noClasses().that().resideInAPackage(ROOT + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "org.hibernate..",
                        "com.nimbusds..",
                        "io.swagger..",
                        "com.fasterxml.."
                )
                .because("frameworks são detalhes: pertencem à infraestrutura e à apresentação")
                .check(CLASSES);
    }

    /**
     * Os modelos do domínio também são as entidades JPA, então as anotações de mapeamento do padrão
     * ({@code jakarta.persistence}) são permitidas. Nada específico de fornecedor (Hibernate, Spring
     * Data) é. O Lombok atua na compilação e só deixa {@code @lombok.Generated} (retenção CLASS,
     * nunca carregada em execução) nos getters e construtores gerados.
     */
    @Test
    void domainDependsOnlyOnTheJpaStandard() {
        noClasses().that().resideInAPackage(ROOT + ".domain..")
                .should().dependOnClassesThat().resideOutsideOfPackages(
                        ROOT + ".domain..", "java..", "jakarta.persistence..", "lombok..")
                .check(CLASSES);
    }

    @Test
    void useCasesAreNamedAfterWhatTheyDo() {
        classes().that().resideInAPackage(ROOT + ".application.usecase..")
                .and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("UseCase")
                .check(CLASSES);
    }

    @Test
    void portsAreInterfacesNamedWithThePortSuffix() {
        classes().that().resideInAPackage(ROOT + ".application.port..")
                .and().areTopLevelClasses()
                .should().beInterfaces()
                .andShould().haveSimpleNameEndingWith("Port")
                .check(CLASSES);
    }

    @Test
    void onlyPortsUseThePortSuffix() {
        noClasses().that().resideOutsideOfPackage(ROOT + ".application.port..")
                .should().haveSimpleNameEndingWith("Port")
                .because("o sufixo distingue a interface (port) das implementações")
                .check(CLASSES);
    }
}
