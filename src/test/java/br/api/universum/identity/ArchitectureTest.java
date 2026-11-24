package br.api.universum.identity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Garante a regra de dependência entre as camadas: as dependências só apontam para dentro.
 *
 * <pre>
 * infrastructure ──► application ──► domain
 * </pre>
 *
 * A aplicação declara as abstrações (interfaces) de que precisa, e a infraestrutura as implementa.
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
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                .check(CLASSES);
    }

    @Test
    void endpointsGoThroughTheUseCases() {
        noClasses().that().resideInAPackage(ROOT + ".infrastructure.endpoints..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".application.abstractions..",
                        ROOT + ".infrastructure.persistence..",
                        ROOT + ".domain.model..")
                .because("os controllers chamam os casos de uso, não os repositórios nem as entidades")
                .check(CLASSES);
    }

    /**
     * A aplicação usa o que o Spring oferece (anotações, transações, eventos, paginação, PasswordEncoder),
     * mas não detalhes de web, de JPA ou de JWT, que ficam na infraestrutura.
     */
    @Test
    void theApplicationDoesNotDependOnInfrastructureTechnologies() {
        noClasses().that().resideInAPackage(ROOT + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.data.jpa..",
                        "org.springframework.security.oauth2..",
                        "jakarta.servlet..",
                        "org.hibernate..",
                        "com.nimbusds..",
                        "io.swagger.."
                )
                .check(CLASSES);
    }

    @Test
    void abstractionsAreInterfaces() {
        classes().that().resideInAPackage(ROOT + ".application.abstractions..")
                .and().areTopLevelClasses()
                .should().beInterfaces()
                .check(CLASSES);
    }

    @Test
    void useCasesAreServicesNamedAfterWhatTheyDo() {
        classes().that().resideInAPackage(ROOT + ".application.usecases..")
                .and().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("UseCase")
                .check(CLASSES);
    }

    @Test
    void controllersLiveInEndpoints() {
        classes().that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage(ROOT + ".infrastructure.endpoints..")
                .check(CLASSES);
    }
}
