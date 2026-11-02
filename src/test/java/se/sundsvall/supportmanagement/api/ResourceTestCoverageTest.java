package se.sundsvall.supportmanagement.api;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBeans;
import org.springframework.web.bind.annotation.RestController;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

/**
 * Holds the resource tests to {@link ResourceTest}: every dependency a resource takes from this service is mocked by
 * it, and no resource test sets up an application context of its own.
 */
class ResourceTestCoverageTest {

	private static final String SERVICE_PACKAGE = "se.sundsvall.supportmanagement";
	private static final String API_PACKAGE = ResourceTestCoverageTest.class.getPackageName();
	private static final Set<Class<? extends Annotation>> OVERRIDES = Set.of(MockitoBean.class, MockitoBeans.class, MockitoSpyBean.class, MockitoSpyBeans.class);

	@Test
	void everyDependencyOfAResourceIsMockedByResourceTest() {
		final var mocked = AnnotatedElementUtils.findMergedRepeatableAnnotations(ResourceTest.class, MockitoBean.class).stream()
			.flatMap(annotation -> Stream.of(annotation.types()))
			.collect(toSet());

		final var unmocked = classesAnnotatedWith(RestController.class).stream()
			.flatMap(resource -> Stream.of(resource.getDeclaredConstructors())
				.flatMap(constructor -> Stream.of(constructor.getParameterTypes()))
				.filter(type -> type.getPackageName().startsWith(SERVICE_PACKAGE))
				.filter(type -> !mocked.contains(type))
				.map(type -> "%s depends on %s, which @ResourceTest does not mock: add it to the types of @ResourceTest".formatted(resource.getSimpleName(), type.getSimpleName())))
			.distinct()
			.toList();

		assertThat(unmocked).isEmpty();
	}

	@Test
	void everyResourceTestIsAnnotatedWithResourceTest() {
		final var elsewhere = classesAnnotatedWith(SpringBootTest.class).stream()
			.filter(testClass -> !AnnotatedElementUtils.hasAnnotation(testClass, ResourceTest.class))
			.map(testClass -> "%s sets up an application context of its own: annotate it with @ResourceTest".formatted(testClass.getSimpleName()))
			.toList();

		assertThat(elsewhere).isEmpty();
	}

	@Test
	void noResourceTestOverridesBeansOfItsOwn() {
		final var overriding = classesAnnotatedWith(ResourceTest.class).stream()
			.filter(ResourceTestCoverageTest::overridesBeansOfItsOwn)
			.map(testClass -> "%s overrides beans of its own, which gives it an application context of its own: add the type to @ResourceTest and autowire it"
				.formatted(testClass.getSimpleName()))
			.toList();

		assertThat(overriding).isEmpty();
	}

	private static boolean overridesBeansOfItsOwn(final Class<?> testClass) {
		return Stream.concat(Stream.of(testClass.getDeclaredAnnotations()), Stream.of(testClass.getDeclaredFields()).flatMap(field -> Stream.of(field.getDeclaredAnnotations())))
			.map(Annotation::annotationType)
			.anyMatch(OVERRIDES::contains);
	}

	/**
	 * The concrete classes of the api package, main and test code alike, carrying the annotation directly or through
	 * another annotation.
	 */
	private static List<Class<?>> classesAnnotatedWith(final Class<? extends Annotation> annotation) {
		final var scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(annotation, true));

		return scanner.findCandidateComponents(API_PACKAGE).stream()
			.map(BeanDefinition::getBeanClassName)
			.<Class<?>>map(throwingFunction(Class::forName))
			.toList();
	}
}
