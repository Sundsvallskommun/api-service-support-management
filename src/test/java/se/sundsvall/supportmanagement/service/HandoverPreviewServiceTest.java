package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.jsonschema.JsonSchema;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreviewRequest;
import se.sundsvall.supportmanagement.api.model.handover.MatchReason;
import se.sundsvall.supportmanagement.api.model.handover.MetadataOption;
import se.sundsvall.supportmanagement.api.model.handover.Warning;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(MockitoExtension.class)
class HandoverPreviewServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String TARGET_NAMESPACE = "OTHER_NAMESPACE";
	private static final String TARGET_MUNICIPALITY_ID = "2262";

	@Mock
	private AccessControlService accessControlServiceMock;
	@Mock
	private ErrandsRepository errandsRepositoryMock;
	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;
	@Mock
	private StatusRepository statusRepositoryMock;
	@Mock
	private CategoryRepository categoryRepositoryMock;
	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;
	@Mock
	private ContactReasonRepository contactReasonRepositoryMock;
	@Mock
	private RoleRepository roleRepositoryMock;
	@Mock
	private JsonSchemaClient jsonSchemaClientMock;

	@InjectMocks
	private HandoverPreviewService service;

	private static HandoverPreviewRequest request() {
		return HandoverPreviewRequest.create()
			.withTargetNamespace(TARGET_NAMESPACE)
			.withTargetMunicipalityId(TARGET_MUNICIPALITY_ID);
	}

	/**
	 * Stubs the target namespace existence check, the source errand load and the target namespace metadata lookups used to
	 * build the candidate lists and the role warnings, plus the source namespace status lookup used to resolve the source
	 * status display name. The authorization gate ({@code verifyExistingErrandAndAuthorization}) is a void method left as a
	 * no-op here.
	 */
	private void stubErrandAndTargetMetadata(final ErrandEntity errand) {
		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));

		// Target namespace metadata (candidates)
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(List.of(StatusEntity.create().withName("IN_PROGRESS").withDisplayName("Pågående")));
		// Source namespace metadata (for resolving the source status display name)
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(MUNICIPALITY_ID), any()))
			.thenReturn(List.of(StatusEntity.create().withName("ONGOING").withDisplayName("Pågående")));
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(List.of(CategoryEntity.create().withName("SUPPORT_CASE").withTypes(List.of(TypeEntity.create().withName("OTHER_ISSUES")))));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID))
			.thenReturn(List.of(MetadataLabelEntity.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard")));
		when(contactReasonRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(List.of(ContactReasonEntity.create().withReason("Bygglov")));
		when(roleRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(List.of(RoleEntity.create().withName("ADMIN")));
	}

	private void verifyAuthorizationAndErrandLoad() {
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(namespaceConfigRepositoryMock).existsByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	private void verifyTargetMetadataReads() {
		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any());
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any());
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID);
		verify(contactReasonRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any());
		verify(roleRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any());
	}

	private void verifySourceStatusRead() {
		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(MUNICIPALITY_ID), any());
	}

	private void verifyNoMoreOnAllMocks() {
		verifyNoMoreInteractions(accessControlServiceMock, errandsRepositoryMock, namespaceConfigRepositoryMock, statusRepositoryMock, categoryRepositoryMock,
			metadataLabelRepositoryMock, contactReasonRepositoryMock, roleRepositoryMock, jsonSchemaClientMock);
	}

	private static ErrandEntity sourceErrand(final List<JsonParameterEntity> jsonParameters) {
		return ErrandEntity.create()
			.withTitle("Trasig dörr")
			.withPriority("HIGH")
			.withStatus("ONGOING")
			.withCategory("SUPPORT_CASE")
			.withType("OTHER_ISSUES")
			.withStakeholders(List.of(StakeholderEntity.create().withRole("EXTERNAL_REPORTER")))
			.withContactReason(ContactReasonEntity.create().withReason("Bygglov"))
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId("uuid-a")))
			.withJsonParameters(jsonParameters);
	}

	@Test
	void previewHandoverToSameNamespaceAndMunicipality() {
		final var request = HandoverPreviewRequest.create()
			.withTargetNamespace(NAMESPACE)
			.withTargetMunicipalityId(MUNICIPALITY_ID);

		final var e = assertThrows(ThrowableProblem.class, () -> service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Target namespace and municipalityId must differ from the source errand");
		verifyNoInteractions(accessControlServiceMock, errandsRepositoryMock, namespaceConfigRepositoryMock, statusRepositoryMock, categoryRepositoryMock,
			metadataLabelRepositoryMock, contactReasonRepositoryMock, roleRepositoryMock, jsonSchemaClientMock);
	}

	@Test
	void previewHandoverWhenNotAuthorized() {
		doThrow(Problem.valueOf(UNAUTHORIZED, "Errand not accessible by user 'user'"))
			.when(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);

		final var request = request();
		final var e = assertThrows(ThrowableProblem.class, () -> service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request));

		assertThat(e.getStatus()).isEqualTo(UNAUTHORIZED);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
		verifyNoInteractions(errandsRepositoryMock, namespaceConfigRepositoryMock, statusRepositoryMock, categoryRepositoryMock,
			metadataLabelRepositoryMock, contactReasonRepositoryMock, roleRepositoryMock, jsonSchemaClientMock);
	}

	@Test
	void previewHandoverWithUnknownTargetNamespace() {
		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(false);

		final var request = request();
		final var e = assertThrows(ThrowableProblem.class, () -> service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Target namespace 'OTHER_NAMESPACE' for municipality '2262' does not exist");
		// Authorization is checked first; the errand is only loaded once the target namespace is known to exist
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(namespaceConfigRepositoryMock).existsByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID);
		verifyNoMoreInteractions(accessControlServiceMock, namespaceConfigRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock, statusRepositoryMock, categoryRepositoryMock, metadataLabelRepositoryMock, contactReasonRepositoryMock, roleRepositoryMock, jsonSchemaClientMock);
	}

	@Test
	void previewHandoverWhenErrandNotFound() {
		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		final var request = request();
		final var e = assertThrows(ThrowableProblem.class, () -> service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request));

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: An errand with id '%s' could not be found in namespace 'namespace' for municipality with id '2281'".formatted(ERRAND_ID));
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(namespaceConfigRepositoryMock).existsByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		// No metadata is read once the errand is known to be missing
		verifyNoMoreInteractions(accessControlServiceMock, namespaceConfigRepositoryMock, errandsRepositoryMock);
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, metadataLabelRepositoryMock, contactReasonRepositoryMock, roleRepositoryMock, jsonSchemaClientMock);
	}

	@Test
	void previewHandoverBuildsCompletePreview() {
		stubErrandAndTargetMetadata(sourceErrand(List.of(
			JsonParameterEntity.create().withKey("orgUnit").withSchemaId("missing-schema"),
			JsonParameterEntity.create().withKey("person").withSchemaId("present-schema"))));

		when(jsonSchemaClientMock.getSchemaById(TARGET_MUNICIPALITY_ID, "present-schema")).thenReturn(new JsonSchema());
		// A 404 (schema not registered) is bypassed to a NOT_FOUND problem rather than wrapped in BAD_GATEWAY
		when(jsonSchemaClientMock.getSchemaById(TARGET_MUNICIPALITY_ID, "missing-schema")).thenThrow(Problem.valueOf(NOT_FOUND, "no such schema"));

		final var result = service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request());

		// directlyCopyable
		assertThat(result.getDirectlyCopyable().getTitle()).isEqualTo("Trasig dörr");
		assertThat(result.getDirectlyCopyable().getPriority()).isEqualTo(Priority.HIGH);
		assertThat(result.getDirectlyCopyable().getStakeholderCount()).isEqualTo(1);
		assertThat(result.getDirectlyCopyable().getExternalTagCount()).isZero();
		assertThat(result.getDirectlyCopyable().getAttachmentCount()).isZero();

		// mappingRequired - status (source display name resolved from source namespace; suggested via display name match)
		assertThat(result.getMappingRequired().getStatus().getSource())
			.isEqualTo(MetadataOption.create().withName("ONGOING").withDisplayName("Pågående"));
		assertThat(result.getMappingRequired().getStatus().getSuggestedTarget()).isEqualTo("IN_PROGRESS");
		assertThat(result.getMappingRequired().getStatus().getMatchReason()).isEqualTo(MatchReason.DISPLAY_NAME_EXACT);
		assertThat(result.getMappingRequired().getStatus().getCandidates())
			.containsExactly(MetadataOption.create().withName("IN_PROGRESS").withDisplayName("Pågående"));

		// mappingRequired - classification (suggested via exact category/type name match)
		assertThat(result.getMappingRequired().getClassification().getSource().getCategory()).isEqualTo("SUPPORT_CASE");
		assertThat(result.getMappingRequired().getClassification().getSource().getType()).isEqualTo("OTHER_ISSUES");
		assertThat(result.getMappingRequired().getClassification().getCandidates()).containsExactly(entry("SUPPORT_CASE", List.of("OTHER_ISSUES")));
		assertThat(result.getMappingRequired().getClassification().getSuggestedCategory()).isEqualTo("SUPPORT_CASE");
		assertThat(result.getMappingRequired().getClassification().getSuggestedType()).isEqualTo("OTHER_ISSUES");

		// mappingRequired - labels (no suggestion: the lazy metadataLabel association is unset on the hand-built errand)
		assertThat(result.getMappingRequired().getLabels().getCandidates()).hasSize(1);
		assertThat(result.getMappingRequired().getLabels().getMappings()).hasSize(1);
		assertThat(result.getMappingRequired().getLabels().getMappings().getFirst().getSourceId()).isEqualTo("uuid-a");
		assertThat(result.getMappingRequired().getLabels().getMappings().getFirst().getSuggestedTargetId()).isNull();
		assertThat(result.getMappingRequired().getLabels().getMappings().getFirst().getMatchReason()).isNull();

		// mappingRequired - contactReason (suggested via exact match)
		assertThat(result.getMappingRequired().getContactReason().getSource()).isEqualTo("Bygglov");
		assertThat(result.getMappingRequired().getContactReason().getCandidates()).containsExactly("Bygglov");
		assertThat(result.getMappingRequired().getContactReason().getSuggested()).isEqualTo("Bygglov");

		// notCopyable
		assertThat(result.getNotCopyable()).extracting("field").containsExactly("phases", "activePhaseId");

		// warnings - unknown role + unregistered parameter schema
		assertThat(result.getWarnings()).containsExactly(
			Warning.roleNotInTarget("EXTERNAL_REPORTER"),
			Warning.parameterSchemaMismatch("orgUnit", "jsonSchema 'missing-schema' not registered in target"));

		verifyAuthorizationAndErrandLoad();
		verifyTargetMetadataReads();
		verifySourceStatusRead();
		verify(jsonSchemaClientMock).getSchemaById(TARGET_MUNICIPALITY_ID, "present-schema");
		verify(jsonSchemaClientMock).getSchemaById(TARGET_MUNICIPALITY_ID, "missing-schema");
		verifyNoMoreOnAllMocks();
	}

	@Test
	void previewHandoverWithMinimalErrandProducesNoWarnings() {
		// An errand with no status, stakeholders, json parameters or classification exercises the null-collection and
		// null-status branches; the source status repository is never queried when the errand has no status.
		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(true);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(ErrandEntity.create().withTitle("Tom")));
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of());
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(List.of());
		when(contactReasonRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of());
		when(roleRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of());

		final var result = service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request());

		assertThat(result.getDirectlyCopyable().getTitle()).isEqualTo("Tom");
		assertThat(result.getDirectlyCopyable().getStakeholderCount()).isZero();
		assertThat(result.getMappingRequired().getStatus().getSource()).isNull();
		assertThat(result.getMappingRequired().getClassification().getSource()).isNull();
		assertThat(result.getMappingRequired().getLabels().getCandidates()).isEmpty();
		assertThat(result.getMappingRequired().getLabels().getMappings()).isEmpty();
		assertThat(result.getMappingRequired().getContactReason().getSource()).isNull();
		assertThat(result.getWarnings()).isEmpty();

		verifyAuthorizationAndErrandLoad();
		verifyTargetMetadataReads();
		verifyNoMoreOnAllMocks();
	}

	@Test
	void previewHandoverWarnsOnlyForRolesAbsentFromTargetAndSkipsBlankSchemaId() {
		// Blank roles are ignored, roles present in the target ("ADMIN") produce no warning, and only roles absent from the
		// target ("EXTERNAL_REPORTER") are reported. A blank schemaId must be skipped without calling the json schema client.
		stubErrandAndTargetMetadata(ErrandEntity.create()
			.withStatus("ONGOING")
			.withStakeholders(List.of(
				StakeholderEntity.create().withRole("   "),
				StakeholderEntity.create().withRole("ADMIN"),
				StakeholderEntity.create().withRole("EXTERNAL_REPORTER")))
			.withJsonParameters(List.of(JsonParameterEntity.create().withKey("orgUnit").withSchemaId("  "))));

		final var result = service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request());

		assertThat(result.getWarnings()).containsExactly(Warning.roleNotInTarget("EXTERNAL_REPORTER"));

		verifyAuthorizationAndErrandLoad();
		verifyTargetMetadataReads();
		verifySourceStatusRead();
		verifyNoMoreOnAllMocks();
	}

	@Test
	void previewHandoverToSameNamespaceDifferentMunicipalityIsAllowed() {
		// The guard only rejects a handover to the exact same namespace AND municipality; a different municipality within the
		// same namespace name is a valid destination.
		final var targetMunicipalityId = "9999";
		final var request = HandoverPreviewRequest.create()
			.withTargetNamespace(NAMESPACE)
			.withTargetMunicipalityId(targetMunicipalityId);

		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, targetMunicipalityId)).thenReturn(true);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(ErrandEntity.create().withTitle("Tom")));
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(targetMunicipalityId), any())).thenReturn(List.of());
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(targetMunicipalityId), any())).thenReturn(List.of());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, targetMunicipalityId)).thenReturn(List.of());
		when(contactReasonRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(targetMunicipalityId), any())).thenReturn(List.of());
		when(roleRepositoryMock.findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(targetMunicipalityId), any())).thenReturn(List.of());

		final var result = service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		assertThat(result.getDirectlyCopyable().getTitle()).isEqualTo("Tom");
		assertThat(result.getWarnings()).isEmpty();

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(namespaceConfigRepositoryMock).existsByNamespaceAndMunicipalityId(NAMESPACE, targetMunicipalityId);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(targetMunicipalityId), any());
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(targetMunicipalityId), any());
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityId(NAMESPACE, targetMunicipalityId);
		verify(contactReasonRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(targetMunicipalityId), any());
		verify(roleRepositoryMock).findAllByNamespaceAndMunicipalityId(eq(NAMESPACE), eq(targetMunicipalityId), any());
		verifyNoMoreOnAllMocks();
	}

	@Test
	void previewHandoverPropagatesNonNotFoundSchemaError() {
		stubErrandAndTargetMetadata(sourceErrand(List.of(
			JsonParameterEntity.create().withKey("orgUnit").withSchemaId("boom-schema"))));

		when(jsonSchemaClientMock.getSchemaById(TARGET_MUNICIPALITY_ID, "boom-schema")).thenThrow(Problem.valueOf(BAD_GATEWAY, "upstream error"));

		final var request = request();
		final var e = assertThrows(ThrowableProblem.class, () -> service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request));

		assertThat(e.getStatus()).isEqualTo(BAD_GATEWAY);
		verifyAuthorizationAndErrandLoad();
		verifyTargetMetadataReads();
		verifySourceStatusRead();
		verify(jsonSchemaClientMock).getSchemaById(TARGET_MUNICIPALITY_ID, "boom-schema");
		verifyNoMoreOnAllMocks();
	}
}
