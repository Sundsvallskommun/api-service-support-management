package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverErrandRequest;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverInclude;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverMapping;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverOverrides;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

import static org.assertj.core.api.Assertions.assertThat;

class HandoverMapperTest {

	private static ErrandEntity minimalEntity() {
		return ErrandEntity.create()
			.withId("source-id")
			.withErrandNumber("KC-23010001")
			.withNamespace("MY_NAMESPACE")
			.withMunicipalityId("2281")
			.withTitle("Source title")
			.withStatus("OPEN")
			.withPriority("MEDIUM")
			.withResolution("OLD_RESOLUTION");
	}

	private static HandoverMapping minimalMapping() {
		return HandoverMapping.create()
			.withStatus("NEW_CASE")
			.withClassification(Classification.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES"))
			.withLabels(List.of());
	}

	@Test
	void buildTargetErrandMinimalRequest() {
		final var source = minimalEntity();
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping());

		final var result = HandoverMapper.buildTargetErrand(source, request);

		assertThat(result.getStatus()).isEqualTo("NEW_CASE");
		assertThat(result.getClassification().getCategory()).isEqualTo("SUPPORT_CASE");
		assertThat(result.getClassification().getType()).isEqualTo("OTHER_ISSUES");
		assertThat(result.getLabels()).isEmpty();
		assertThat(result.getId()).isNull();
		assertThat(result.getErrandNumber()).isNull();
		assertThat(result.getResolution()).isNull();
		assertThat(result.getSuspension()).isNull();
		assertThat(result.getStakeholders()).isNull();
		assertThat(result.getExternalTags()).isNull();
		assertThat(result.getParameters()).isNull();
		assertThat(result.getJsonParameters()).isNull();
		assertThat(result.getBusinessRelated()).isNull();
		assertThat(result.getEscalationEmail()).isNull();
		assertThat(result.getContactReasonDescription()).isNull();
		assertThat(result.getAssignedUserId()).isNull();
		assertThat(result.getAssignedGroupId()).isNull();
	}

	@Test
	void buildTargetErrandWithOverrides() {
		final var source = minimalEntity()
			.withTitle("Original title")
			.withDescription("Original desc")
			.withPriority("LOW");
		final var overrides = HandoverOverrides.create()
			.withTitle("Override title")
			.withDescription("Override desc")
			.withPriority(Priority.HIGH)
			.withAssignedUserId("user-123")
			.withAssignedGroupId("group-456");
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping())
			.withOverrides(overrides);

		final var result = HandoverMapper.buildTargetErrand(source, request);

		assertThat(result.getTitle()).isEqualTo("Override title");
		assertThat(result.getDescription()).isEqualTo("Override desc");
		assertThat(result.getPriority()).isEqualTo(Priority.HIGH);
		assertThat(result.getAssignedUserId()).isEqualTo("user-123");
		assertThat(result.getAssignedGroupId()).isEqualTo("group-456");
	}

	@Test
	void buildTargetErrandWithIncludeFlags() {
		final var source = minimalEntity()
			.withStakeholders(List.of(StakeholderEntity.create().withRole("APPLICANT").withExternalId("ext-1")))
			.withBusinessRelated(true)
			.withEscalationEmail("mail@example.com")
			.withContactReasonDescription("some reason");
		final var include = HandoverInclude.create()
			.withStakeholders(true)
			.withExternalTags(false)
			.withParameters(false)
			.withJsonParameters(false)
			.withBusinessRelated(false)
			.withEscalationEmail(false)
			.withContactReasonDescription(false);
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping())
			.withInclude(include);

		final var result = HandoverMapper.buildTargetErrand(source, request);

		assertThat(result.getStakeholders()).isNotNull().hasSize(1);
		assertThat(result.getExternalTags()).isNull();
		assertThat(result.getParameters()).isNull();
		assertThat(result.getJsonParameters()).isNull();
		assertThat(result.getBusinessRelated()).isNull();
		assertThat(result.getEscalationEmail()).isNull();
		assertThat(result.getContactReasonDescription()).isNull();
	}

	@Test
	void buildTargetErrandChannelMappingExplicit() {
		final var source = minimalEntity().withChannel("EMAIL");
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping().withChannel("WEB_UI"));

		final var result = HandoverMapper.buildTargetErrand(source, request);

		assertThat(result.getChannel()).isEqualTo("WEB_UI");
	}

	@Test
	void buildTargetErrandChannelNullMappingCopiesFromSource() {
		final var source = minimalEntity().withChannel("EMAIL");
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping());

		final var result = HandoverMapper.buildTargetErrand(source, request);

		assertThat(result.getChannel()).isEqualTo("EMAIL");
	}

	@Test
	void buildAppliedMappingsAllFields() {
		final var mapping = HandoverMapping.create()
			.withStatus("NEW_CASE")
			.withClassification(Classification.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES"))
			.withLabels(List.of("label-1"))
			.withContactReason("Printer issue")
			.withChannel("WEB_UI");
		final var request = HandoverErrandRequest.create().withMapping(mapping);

		final var result = HandoverMapper.buildAppliedMappings(request);

		assertThat(result).containsKey("status")
			.containsKey("classification.category")
			.containsKey("classification.type")
			.containsKey("labels")
			.containsKey("contactReason")
			.containsKey("channel");
		assertThat(result.get("status")).isEqualTo("NEW_CASE");
		assertThat(result.get("classification.category")).isEqualTo("SUPPORT_CASE");
		assertThat(result.get("classification.type")).isEqualTo("OTHER_ISSUES");
		assertThat(result.get("contactReason")).isEqualTo("Printer issue");
		assertThat(result.get("channel")).isEqualTo("WEB_UI");
	}

	@Test
	void buildAppliedMappingsPartialFields() {
		final var mapping = HandoverMapping.create()
			.withStatus("NEW_CASE")
			.withClassification(Classification.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES"))
			.withLabels(List.of());
		final var request = HandoverErrandRequest.create().withMapping(mapping);

		final var result = HandoverMapper.buildAppliedMappings(request);

		assertThat(result).containsOnlyKeys("status", "classification.category", "classification.type", "labels");
	}

	@Test
	void buildWarningsWithParameters() {
		final var source = minimalEntity()
			.withParameters(List.of(ParameterEntity.create().withKey("key")));
		final var include = HandoverInclude.create().withParameters(true);
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping())
			.withInclude(include);

		final var warnings = HandoverMapper.buildWarnings(source, request);

		assertThat(warnings).anyMatch(w -> w.contains("Parameters were copied"));
	}

	@Test
	void buildWarningsWithJsonParameters() {
		final var source = minimalEntity()
			.withJsonParameters(List.of(JsonParameterEntity.create().withKey("key")));
		final var include = HandoverInclude.create().withJsonParameters(true);
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping())
			.withInclude(include);

		final var warnings = HandoverMapper.buildWarnings(source, request);

		assertThat(warnings).anyMatch(w -> w.contains("JSON parameters were copied"));
	}

	@Test
	void buildWarningsNoInclude() {
		final var source = minimalEntity()
			.withParameters(List.of(ParameterEntity.create().withKey("key")))
			.withJsonParameters(List.of(JsonParameterEntity.create().withKey("key")))
			.withStakeholders(List.of(StakeholderEntity.create().withRole("APPLICANT")))
			.withChannel("EMAIL");
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping().withChannel("WEB_UI"));

		final var warnings = HandoverMapper.buildWarnings(source, request);

		assertThat(warnings).isEmpty();
	}

	@Test
	void buildWarningsChannelNotMapped() {
		final var source = minimalEntity().withChannel("EMAIL");
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping());

		final var warnings = HandoverMapper.buildWarnings(source, request);

		assertThat(warnings).anyMatch(w -> w.contains("Channel was copied from source errand"));
	}

	@Test
	void buildWarningsWithStakeholders() {
		final var source = minimalEntity()
			.withStakeholders(List.of(StakeholderEntity.create().withRole("APPLICANT")));
		final var include = HandoverInclude.create().withStakeholders(true);
		final var request = HandoverErrandRequest.create()
			.withMapping(minimalMapping())
			.withInclude(include);

		final var warnings = HandoverMapper.buildWarnings(source, request);

		assertThat(warnings).anyMatch(w -> w.contains("Stakeholders were copied"));
	}

	@Test
	void encodeWarningsEmpty() {
		final var result = HandoverMapper.encodeWarnings(List.of());

		assertThat(result).isEmpty();
	}

	@Test
	void encodeWarningsSingle() {
		final var result = HandoverMapper.encodeWarnings(List.of("one warning"));

		assertThat(result).isEqualTo("one warning");
	}

	@Test
	void encodeWarningsMultiple() {
		final var result = HandoverMapper.encodeWarnings(List.of("first warning", "second warning"));

		assertThat(result).isEqualTo("first warning|~|second warning");
	}

	@Test
	void decodeWarningsNull() {
		final var result = HandoverMapper.decodeWarnings(null);

		assertThat(result).isEmpty();
	}

	@Test
	void decodeWarningsBlank() {
		final var result = HandoverMapper.decodeWarnings("   ");

		assertThat(result).isEmpty();
	}

	@Test
	void decodeWarningsMultiple() {
		final var result = HandoverMapper.decodeWarnings("first warning|~|second warning");

		assertThat(result).containsExactly("first warning", "second warning");
	}
}
