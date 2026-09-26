package com.example.ssms.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTests {

	private static JavaClasses importedClasses;

	@BeforeAll
	static void setup() {
		importedClasses = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages("com.example.ssms");
	}

	@Test
	@DisplayName("Controllers must reside in a controller package and be annotated with @RestController")
	void controllersShouldBeInControllerPackage() {
		classes().that().haveSimpleNameEndingWith("Controller").should().resideInAPackage("..controller..").andShould()
				.beAnnotatedWith(RestController.class).check(importedClasses);
	}

	@Test
	@DisplayName("Services must reside in a service package and be annotated with @Service")
	void servicesShouldBeInServicePackage() {
		classes().that().haveSimpleNameEndingWith("Service").and().doNotHaveSimpleName("UserPrincipalDetailsService")
				.should().resideInAnyPackage("..service..", "..jwt..").check(importedClasses);
	}

	@Test
	@DisplayName("Repositories must reside in a repository package and be interfaces")
	void repositoriesShouldBeInRepositoryPackage() {
		classes().that().haveSimpleNameEndingWith("Repository").should().resideInAPackage("..repository..").andShould()
				.beInterfaces().check(importedClasses);
	}

	@Test
	@DisplayName("Controllers should never access repositories directly")
	void controllersShouldNotAccessRepositoriesDirectly() {
		noClasses().that().resideInAPackage("..controller..").should().dependOnClassesThat()
				.resideInAPackage("..repository..").check(importedClasses);
	}
}
