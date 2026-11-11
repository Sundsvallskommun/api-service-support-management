package se.sundsvall.supportmanagement.service.access;

import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant.LabelRoute;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant.ReporterRoute;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.ATTACHMENT;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.MEASURE;

class NamespaceGrantTest {

	private static final Set<MetadataLabelEntity> LABELS = Set.of(MetadataLabelEntity.create().withId("label-id"));

	@Test
	void reachesOnEveryRouteWhenNothingIsEnforced() {
		assertThat(NamespaceGrant.UNRESTRICTED.reachesOnEveryRoute(MEASURE)).isTrue();
	}

	@Test
	void reachesOnEveryRouteWhenEveryRouteReachesTheResource() {
		final var grant = new NamespaceGrant(true,
			new LabelRoute(LABELS, null, Set.of(MEASURE, ATTACHMENT)),
			new LabelRoute(LABELS, null, Set.of(MEASURE)),
			new ReporterRoute("joe01doe", null, Set.of(MEASURE)));

		assertThat(grant.reachesOnEveryRoute(MEASURE)).isTrue();
	}

	/** The listing answers across every route at once, so one route lacking the resource refuses it. */
	@Test
	void doesNotReachOnEveryRouteWhenOneRouteLacksTheResource() {
		final var grant = new NamespaceGrant(true,
			new LabelRoute(LABELS, null, Set.of(MEASURE, ATTACHMENT)),
			new LabelRoute(LABELS, null, Set.of(MEASURE)),
			new ReporterRoute("joe01doe", null, Set.of(MEASURE)));

		assertThat(grant.reachesOnEveryRoute(ATTACHMENT)).isFalse();
		// Whereas some route reaching it is enough for reaches
		assertThat(grant.reaches(ATTACHMENT)).isTrue();
	}

	@Test
	void aReporterRouteAloneLackingTheResourceRefusesIt() {
		final var grant = new NamespaceGrant(true, new LabelRoute(LABELS, null, Set.of(MEASURE)), null, new ReporterRoute("joe01doe", null, Set.of()));

		assertThat(grant.reachesOnEveryRoute(MEASURE)).isFalse();
	}

	/** A route reaching no errand cannot reveal anything about the errands, so it refuses nothing. */
	@Test
	void routesReachingNothingAreLeftOut() {
		final var grant = new NamespaceGrant(true,
			new LabelRoute(LABELS, null, Set.of(MEASURE)),
			new LabelRoute(Set.of(), null, Set.of()),
			new ReporterRoute(null, null, Set.of()));

		assertThat(grant.reachesOnEveryRoute(MEASURE)).isTrue();
	}

	@Test
	void aGrantReachingNoErrandRefusesNothing() {
		final var grant = new NamespaceGrant(true, null, null, null);

		assertThat(grant.reachesOnEveryRoute(MEASURE)).isTrue();
	}
}
