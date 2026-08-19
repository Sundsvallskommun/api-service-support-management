package se.sundsvall.supportmanagement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.service.AccessControlService;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

/**
 * Reminds developers to route errand access through {@link AccessControlService}.
 * <p>
 * Reaching {@link ErrandsRepository} directly is how access control gets forgotten: the errand is fetched, the caller
 * is
 * never checked, and nothing fails. A new user facing service therefore has to make a deliberate choice here rather
 * than silently bypassing the guard.
 * <p>
 * This is an early warning, not a proof of correctness. Holding an {@link AccessControlService} says nothing about
 * whether it is actually consulted on every path, so code review and acceptance testing in a test environment are
 * still required.
 */
class AccessControlChokePointTest {

	/**
	 * Components that legitimately reach errands without an access check, with the reason they are exempt. Add to this
	 * list only when the component genuinely has no user to authorize.
	 */
	private static final Set<Class<?>> EXEMPT = Set.of(
		// The choke point itself.
		AccessControlService.class,

		// Scheduled workers and system triggered actions. These run without an Identifier, so there is no user to
		// authorize and access control does not apply.
		se.sundsvall.supportmanagement.service.action.AddLabelAction.class,
		se.sundsvall.supportmanagement.service.scheduler.action.ActionWorker.class,
		se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderWorker.class,
		se.sundsvall.supportmanagement.service.scheduler.messageexchange.MessageExchangeWorker.class,
		se.sundsvall.supportmanagement.service.scheduler.notificationdispatch.NotificationDispatchWorker.class,
		se.sundsvall.supportmanagement.service.scheduler.supensions.SuspensionWorker.class,
		se.sundsvall.supportmanagement.service.scheduler.webmessagecollector.WebMessageCollectorWorker.class,
		se.sundsvall.supportmanagement.service.MessageExchangeSyncService.class,

		// MetadataService only reads errands to check whether metadata is referenced. It cannot inject
		// AccessControlService without creating a circular dependency
		// (metadataService -> accessControlService -> accessMapperService -> metadataService), which is why the
		// metadata endpoints are guarded in the resource layer instead.
		se.sundsvall.supportmanagement.service.MetadataService.class,

		// SubscriptionService reaches the errand only to attach it to a subscription. Subscribing deliberately does not
		// require access to the errand - subscribing a colleague is a supported workflow, and a subscriber only ever
		// receives notifications for errands they may reach themselves, because dispatch filters on access. Reading and
		// deleting subscriptions is restricted by ownership instead, which the access mapper says nothing about.
		se.sundsvall.supportmanagement.service.SubscriptionService.class);

	@Test
	void errandAccessGoesThroughAccessControlService() {
		final var offenders = componentsInjecting(ErrandsRepository.class)
			.filter(type -> !EXEMPT.contains(type))
			.filter(type -> !injects(type, AccessControlService.class))
			.map(Class::getName)
			.sorted()
			.toList();

		assertThat(offenders)
			.withFailMessage(() -> """
				The following classes inject ErrandsRepository without also injecting AccessControlService:

				%s

				Errands must be reached through AccessControlService (getErrand or
				verifyExistingErrandAndAuthorization), naming the ProtectedResource and the lowest acceptable access
				level. Fetching an errand straight from the repository skips access control silently.

				If the class genuinely has no user to authorize, such as a scheduled worker, add it to EXEMPT in
				%s together with the reason.""".formatted(
				offenders.stream().map(name -> "  - " + name).collect(joining("\n")),
				getClass().getName()))
			.isEmpty();
	}

	@Test
	void exemptionsAreStillInUse() {
		// Keeps EXEMPT from accumulating stale entries as classes stop using the repository.
		final var injecting = componentsInjecting(ErrandsRepository.class).collect(toSet());

		assertThat(EXEMPT.stream().filter(type -> !injecting.contains(type)).map(Class::getName).sorted().toList())
			.withFailMessage("These EXEMPT entries no longer inject ErrandsRepository and should be removed from %s"
				.formatted(getClass().getName()))
			.isEmpty();
	}

	private Stream<Class<?>> componentsInjecting(final Class<?> dependency) {
		final var scanner = new ClassPathScanningCandidateComponentProvider(true);
		return scanner.findCandidateComponents(getClass().getPackageName()).stream()
			.map(BeanDefinition::getBeanClassName)
			.filter(Objects::nonNull)
			.map(throwingFunction(Class::forName))
			.filter(type -> injects(type, dependency));
	}

	private boolean injects(final Class<?> type, final Class<?> dependency) {
		return declaredFieldTypes(type).anyMatch(dependency::isAssignableFrom);
	}

	private Stream<Class<?>> declaredFieldTypes(final Class<?> type) {
		final var types = new ArrayList<Class<?>>();
		for (var current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			types.addAll(List.of(current.getDeclaredFields()).stream().map(Field::getType).toList());
		}
		return types.stream();
	}
}
