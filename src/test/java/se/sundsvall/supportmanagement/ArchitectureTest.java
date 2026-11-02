package se.sundsvall.supportmanagement;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.Entity;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The architecture and the conventions of the service, held by the build rather than by review.
 */
@AnalyzeClasses(packages = "se.sundsvall.supportmanagement", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	// Layered architecture: api -> service -> integration, never backwards or past a layer. The API models are a layer of
	// their own, since the mappers of the service layer map to and from them, and so is the validation the API models are
	// annotated with. The enums every layer shares live in the model package, outside the layers
	@ArchTest
	static final ArchRule layers = layeredArchitecture()
		.consideringOnlyDependenciesInLayers()
		.layer("Api").definedBy("se.sundsvall.supportmanagement.api", "..api.util..")
		.layer("ApiValidation").definedBy("..api.validation..")
		.layer("ApiModel").definedBy("..api.model..")
		.layer("Service").definedBy("..service..")
		.layer("Integration").definedBy("..integration..")
		.whereLayer("Api").mayNotBeAccessedByAnyLayer()
		.whereLayer("ApiValidation").mayOnlyBeAccessedByLayers("Api", "ApiModel")
		.whereLayer("ApiModel").mayOnlyBeAccessedByLayers("Api", "ApiValidation", "Service")
		.whereLayer("Service").mayOnlyBeAccessedByLayers("Api", "ApiValidation")
		.whereLayer("Integration").mayOnlyBeAccessedByLayers("Service");

	// The service layer mapping to and from the API models is the convention the layers allow, and not a cycle
	@ArchTest
	static final ArchRule noCycles = slices().matching("se.sundsvall.supportmanagement.(*)..").should().beFreeOfCycles()
		.ignoreDependency(alwaysTrue(), resideInAPackage("..api.model.."));

	// Entities must not leak into the API
	@ArchTest
	static final ArchRule noEntitiesInApi = noClasses().that().resideInAPackage("..api..")
		.should().dependOnClassesThat().areAnnotatedWith(Entity.class);

	// The service layer must not know about the web, apart from the file uploaded and the response an attachment is
	// streamed to
	@ArchTest
	static final ArchRule serviceWithoutWeb = noClasses().that().resideInAPackage("..service..")
		.should().dependOnClassesThat(resideInAnyPackage("org.springframework.web..", "jakarta.servlet..")
			.and(not(belongToAnyOf(MultipartFile.class, HttpServletResponse.class, ServletOutputStream.class))));

	// @Transactional only in the service layer, and on the repositories and database utilities of the integration layer
	@ArchTest
	static final ArchRule transactionalMethods = noMethods()
		.that().areDeclaredInClassesThat().resideOutsideOfPackages("..service..", "..integration.db..")
		.should().beAnnotatedWith(Transactional.class);

	@ArchTest
	static final ArchRule transactionalClasses = noClasses()
		.that().resideOutsideOfPackages("..service..", "..integration.db..")
		.should().beAnnotatedWith(Transactional.class);

	// Naming and placement
	@ArchTest
	static final ArchRule resources = classes().that().areAnnotatedWith(RestController.class)
		.should().haveSimpleNameEndingWith("Resource")
		.andShould().resideInAPackage("..api..");

	@ArchTest
	static final ArchRule repositories = classes().that().haveSimpleNameEndingWith("Repository")
		.should().resideInAPackage("..integration..");

	@ArchTest
	static final ArchRule entities = classes().that().areAnnotatedWith(Entity.class)
		.should().haveSimpleNameEndingWith("Entity");

	// Every repository has a circuit breaker
	@ArchTest
	static final ArchRule repositoriesHaveCircuitBreaker = classes().that().haveSimpleNameEndingWith("Repository").and().areInterfaces()
		.should().beAnnotatedWith(CircuitBreaker.class);

	// Resources are package-protected: the class, its constructors and its methods
	@ArchTest
	static final ArchRule resourceClassesNotPublic = classes().that().areAnnotatedWith(RestController.class)
		.should().notBePublic();

	@ArchTest
	static final ArchRule resourceConstructorsNotPublic = noConstructors().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
		.should().bePublic();

	@ArchTest
	static final ArchRule resourceMethodsNotPublic = noMethods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
		.should().bePublic();

	// No time read in the default time zone without saying so
	@ArchTest
	static final ArchRule noLocalDateTimeNow = noClasses()
		.should().callMethod(LocalDateTime.class, "now")
		.orShould().callMethod(LocalDate.class, "now");

	// General rules catching common bad habits
	@ArchTest
	static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

	@ArchTest
	static final ArchRule noStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

	@ArchTest
	static final ArchRule noGenericExceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

	@ArchTest
	static final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

	@ArchTest
	static final ArchRule noJodaTime = NO_CLASSES_SHOULD_USE_JODATIME;
}
