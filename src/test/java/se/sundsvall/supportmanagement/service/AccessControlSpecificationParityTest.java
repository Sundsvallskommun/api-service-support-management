package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.api.model.config.ResourceAccess;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.AccessSnapshot;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;

/**
 * Holds the in memory answer of {@link AccessControlService#resolveErrandAccess} to the specification
 * {@link AccessControlService#withAccessControl} builds, which guards every endpoint.
 * <p>
 * Every combination is put to both, the reported level is held against each of the three required levels, and the two
 * are required to agree.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql"
}, executionPhase = BEFORE_TEST_CLASS)
@Transactional
class AccessControlSpecificationParityTest {

	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String AD_ACCOUNT = "joe01doe";

	private static final MetadataLabelEntity LABEL = MetadataLabelEntity.create().withId("label-id-1");
	private static final MetadataLabelEntity OTHER_LABEL = MetadataLabelEntity.create().withId("label-id-2");

	@Autowired
	private ErrandsRepository errandsRepository;

	private AccessMapperService accessMapperServiceMock;
	private NamespaceConfigService namespaceConfigServiceMock;
	private AccessControlService accessControlService;

	@BeforeEach
	void setUp() {
		accessMapperServiceMock = mock(AccessMapperService.class);
		namespaceConfigServiceMock = mock(NamespaceConfigService.class);
		accessControlService = new AccessControlService(accessMapperServiceMock, namespaceConfigServiceMock, errandsRepository);
	}

	/**
	 * Every shape a user and an errand can meet in: labelled or not, reported by the user or not, granted labels at each
	 * level or none at all, with the reporter exception configured or left out.
	 */
	static Stream<Arguments> combinations() {
		final List<Set<MetadataLabelEntity>> grantedLabels = List.of(Set.of(), Set.of(LABEL), Set.of(OTHER_LABEL), Set.of(LABEL, OTHER_LABEL));

		return Stream.of(true, false).flatMap(labelled -> Stream.of(true, false).flatMap(reporter -> grantedLabels.stream().flatMap(labels -> levelsGranting(labels)
			.flatMap(grantedAt -> Stream.of(true, false).flatMap(reporterAccess -> Stream.of(true, false)
				.map(resourceAccessControl -> Arguments.of(labelled, reporter, labels, grantedAt, reporterAccess, resourceAccessControl)))))));
	}

	@ParameterizedTest(name = "labelled={0} reporter={1} labels={2} grantedAt={3} reporterAccess={4} resourceAccessControl={5}")
	@MethodSource("combinations")
	void reportedLevelMatchesTheSpecification(final boolean labelled, final boolean reporter, final Set<MetadataLabelEntity> grantedLabels, final Access.AccessLevelEnum grantedAt, final boolean reporterAccess, final boolean resourceAccessControl) {

		final var errand = persistErrand(labelled, reporter);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config(reporterAccess, resourceAccessControl));
		when(accessMapperServiceMock.getAccessSnapshot(any(), any(), any())).thenReturn(snapshot(grantedLabels, grantedAt, resourceAccessControl));

		final var reported = reportedErrandLevel(errand);

		Stream.of(LR, R, RW).forEach(required -> {
			final var specificationAllows = errandsRepository.exists(withId(errand.getId())
				.and(accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.ERRAND, required)));

			assertThat(satisfiedBy(reported, required))
				.as("reported level %s against required %s", reported, required)
				.isEqualTo(specificationAllows);
		});
	}

	/**
	 * Every shape a user and a resource of an errand can meet in, leaving out whether the user reported the errand.
	 */
	static Stream<Arguments> resourceCombinations() {
		final List<Set<MetadataLabelEntity>> grantedLabels = List.of(Set.of(), Set.of(LABEL), Set.of(OTHER_LABEL));

		return Stream.of(ProtectedResource.CONVERSATION_MESSAGE, ProtectedResource.NOTE)
			.flatMap(resource -> Stream.of(true, false).flatMap(labelled -> grantedLabels.stream().flatMap(labels -> levelsGranting(labels).flatMap(grantedAt -> Stream.of(true, false)
				.map(resourceAccessControl -> Arguments.of(resource, labelled, labels, grantedAt, resourceAccessControl))))));
	}

	/**
	 * The levels sent in labels are granted at. No labels at all are asked about once, since the snapshot they give is
	 * the same whatever level they are said to be granted at.
	 */
	private static Stream<Access.AccessLevelEnum> levelsGranting(final Set<MetadataLabelEntity> labels) {
		return labels.isEmpty() ? Stream.of(LR) : Stream.of(LR, R, RW);
	}

	/**
	 * The same question asked of a resource of the errand, where the level demanded of the labels follows what the
	 * resource grant carries.
	 */
	@ParameterizedTest(name = "resource={0} labelled={1} labels={2} grantedAt={3} resourceAccessControl={4}")
	@MethodSource("resourceCombinations")
	void reportedResourceLevelMatchesTheSpecification(final ProtectedResource resource, final boolean labelled, final Set<MetadataLabelEntity> grantedLabels, final Access.AccessLevelEnum grantedAt, final boolean resourceAccessControl) {

		final var errand = persistErrand(labelled, false);

		// NOTE is reached at limited read and CONVERSATION_MESSAGE is not, so the floor, what the grant carries and their
		// composition are all exercised.
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config(false, resourceAccessControl)
			.withLimitedReadAccess(LimitedReadAccess.create().withResources(List.of(ProtectedResource.NOTE))));
		when(accessMapperServiceMock.getAccessSnapshot(any(), any(), any())).thenReturn(snapshot(grantedLabels, grantedAt,
			resourceAccessControl ? Map.of(ProtectedResource.ERRAND, RW, resource, RW) : Map.of()));

		final var reported = reportedResourceLevel(errand, resource);

		Stream.of(LR, R, RW).forEach(required -> {
			final var specificationAllows = errandsRepository.exists(withId(errand.getId())
				.and(accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), resource, required)));

			assertThat(satisfiedBy(reported, required))
				.as("reported level %s of %s against required %s", reported, resource, required)
				.isEqualTo(specificationAllows);
		});
	}

	/**
	 * The level the report gives one resource of the errand, or null where it gives none - including where it refuses
	 * the user the errand altogether.
	 */
	private Access.AccessLevelEnum reportedResourceLevel(final ErrandEntity errand, final ProtectedResource resource) {
		try {
			return accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), errand).resources().get(resource);
		} catch (final ThrowableProblem e) {
			return null;
		}
	}

	/**
	 * The level the report gives the errand, or null where it refuses the user altogether.
	 */
	private Access.AccessLevelEnum reportedErrandLevel(final ErrandEntity errand) {
		try {
			return accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), errand).errandLevel();
		} catch (final ThrowableProblem e) {
			return null;
		}
	}

	private static boolean satisfiedBy(final Access.AccessLevelEnum reported, final Access.AccessLevelEnum required) {
		return switch (required) {
			case LR -> reported != null;
			case R -> reported == R || reported == RW;
			case RW -> reported == RW;
		};
	}

	private ErrandEntity persistErrand(final boolean labelled, final boolean reporter) {
		// The id is left to the generator, since setting one makes the entity detached and refuses to persist.
		final var errand = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withErrandNumber("PARITY-" + randomUUID())
			.withTitle("title")
			.withReporterUserId(reporter ? AD_ACCOUNT : "someone01else")
			.withAccessLabels(labelled ? List.of(AccessLabelEmbeddable.create().withMetadataLabelId(LABEL.getId())) : List.of());

		return errandsRepository.saveAndFlush(errand);
	}

	private static NamespaceConfig config(final boolean reporterAccess, final boolean resourceAccessControl) {
		final var config = NamespaceConfig.create()
			.withAccessControl(true)
			.withResourceAccessControl(resourceAccessControl);

		return reporterAccess ? config.withReporterAccess(ReporterAccess.create()
			.withResources(List.of(ResourceAccess.create().withResource(ProtectedResource.ERRAND).withLevel(AccessLevel.R)))) : config;
	}

	/**
	 * Grants sent in labels at sent in level and every level above it, which is how the access mapper answers for a user
	 * held in one access group.
	 */
	private static AccessSnapshot snapshot(final Set<MetadataLabelEntity> labels, final Access.AccessLevelEnum grantedAt, final boolean resourceAccessControl) {
		// Granted at read/write so that the resource never becomes the narrower of the two and the labels stay the thing
		// being compared, while still exercising the branch weighing resources at all.
		return snapshot(labels, grantedAt, resourceAccessControl ? Map.of(ProtectedResource.ERRAND, RW) : Map.of());
	}

	private static AccessSnapshot snapshot(final Set<MetadataLabelEntity> labels, final Access.AccessLevelEnum grantedAt, final Map<ProtectedResource, Access.AccessLevelEnum> resources) {
		return new AccessSnapshot(
			Map.of(
				LR, LR == grantedAt ? labels : Set.of(),
				R, R == grantedAt ? labels : Set.of(),
				RW, RW == grantedAt ? labels : Set.of()),
			Set.of(),
			resources);
	}

	private static Identifier adUser() {
		return Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(AD_ACCOUNT);
	}
}
