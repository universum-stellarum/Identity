package br.api.universum.identity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Guards the dependency rule of the clean architecture: source code dependencies point inwards only.
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
    void domainAndApplicationDoNotDependOnFrameworks() {
        noClasses().that().resideInAnyPackage(ROOT + ".domain..", ROOT + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "org.hibernate..",
                        "com.nimbusds..",
                        "io.swagger..",
                        "com.fasterxml.."
                )
                .because("frameworks are details: they belong to infrastructure and presentation")
                .check(CLASSES);
    }

    @Test
    void domainDoesNotDependOnLibrariesAtAll() {
        noClasses().that().resideInAPackage(ROOT + ".domain..")
                .should().dependOnClassesThat().resideOutsideOfPackages(ROOT + ".domain..", "java..")
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
    void portsAreInterfaces() {
        classes().that().resideInAPackage(ROOT + ".application.port..")
                .and().areTopLevelClasses()
                .should().beInterfaces()
                .check(CLASSES);
    }
}
