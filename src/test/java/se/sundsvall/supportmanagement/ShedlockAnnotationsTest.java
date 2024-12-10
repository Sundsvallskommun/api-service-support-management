package se.sundsvall.supportmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.scheduling.annotation.Scheduled;

class ShedlockAnnotationsTest {

	@Test
	void verifyMandatoryShedlockAnnotations() {
		final var scanner = new ClassPathScanningCandidateComponentProvider(true);
		final var candidates = scanner.findCandidateComponents(this.getClass().getPackageName());
		final var hasEnableSchedulerLock = hasEnableSchedulerLock(candidates);

		candidates.stream()
			.map(this::getMethodsAnnotatedWith)
			.flatMap(m -> m.entrySet().stream())
			.forEach(set -> this.verifyAnnotations(hasEnableSchedulerLock, set));
	}

	private boolean hasEnableSchedulerLock(final Set<BeanDefinition> candidates) {
		return candidates.stream()
			.map(BeanDefinition::getBeanClassName)
			.map(throwingFunction(Class::forName))
			.map(c -> c.getAnnotationsByType(EnableSchedulerLock.class))
			.anyMatch(matches -> matches.length > 0);
	}

	private void verifyAnnotations(final boolean hasEnableSchedulerLockAnnotation, final Entry<String, List<Method>> entrySet) {
		entrySet.getValue().forEach(method -> {
			// Verify that method annotated with Scheduled is also annotated with SchedulerLock
			assertThat(method.isAnnotationPresent(SchedulerLock.class))
				.withFailMessage(() -> "Method %s in class %s has @Scheduled annotation but no @SchedulerLock annotation".formatted(method.getName(), entrySet.getKey()))
				.isTrue();

			assertThat(hasEnableSchedulerLockAnnotation)
				.withFailMessage(() -> "Service contains at least one method annotated with @Scheduled and @SchedulerLock but no @EnableSchedulerLock annotation is present")
				.isTrue();
		});
	}

	private Map<String, List<Method>> getMethodsAnnotatedWith(final BeanDefinition candidate) {
		try {
			final List<Method> methods = new ArrayList<>();
			var klazz = Class.forName(candidate.getBeanClassName());
			while (klazz != Object.class) {
				// need to traverse a type hierarchy in order to process methods from super types iterate though the list of methods
				// declared in the class represented by klass variable, and add those annotated with the specified annotation
				for (final Method method : klazz.getDeclaredMethods()) {
					if (method.isAnnotationPresent(Scheduled.class)) {
						methods.add(method);
					}
				}
				// move to the upper class in the hierarchy in search for more methods
				klazz = klazz.getSuperclass();
			}
			return Map.of(Objects.requireNonNull(candidate.getBeanClassName()), methods);
		} catch (final ClassNotFoundException e) {
			fail("Couldn't traverse class methods as class %s could not be found".formatted(candidate.getBeanClassName()));
			return Collections.emptyMap();
		}
	}

}
