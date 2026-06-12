package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverErrandRequest;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverInclude;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

public final class HandoverMapper {

	private HandoverMapper() {}

	public static Errand buildTargetErrand(final ErrandEntity source, final HandoverErrandRequest request) {
		final var errand = ErrandMapper.toErrand(source);
		final var mapping = request.getMapping();
		final var overrides = request.getOverrides();
		final var include = ofNullable(request.getInclude()).orElseGet(HandoverInclude::create);

		errand.setStatus(mapping.getStatus());
		errand.setClassification(mapping.getClassification());
		errand.setLabels(toErrandLabels(mapping.getLabels()));
		errand.setContactReason(mapping.getContactReason());
		ofNullable(mapping.getChannel()).ifPresent(errand::setChannel);

		errand.setAssignedUserId(nonNull(overrides) ? overrides.getAssignedUserId() : null);
		errand.setAssignedGroupId(nonNull(overrides) ? overrides.getAssignedGroupId() : null);
		if (nonNull(overrides)) {
			ofNullable(overrides.getTitle()).ifPresent(errand::setTitle);
			ofNullable(overrides.getDescription()).ifPresent(errand::setDescription);
			ofNullable(overrides.getPriority()).ifPresent(errand::setPriority);
		}

		if (!include.isStakeholders()) {
			errand.setStakeholders(null);
		}
		if (!include.isExternalTags()) {
			errand.setExternalTags(null);
		}
		if (!include.isParameters()) {
			errand.setParameters(null);
		}
		if (!include.isJsonParameters()) {
			errand.setJsonParameters(null);
		}
		if (!include.isBusinessRelated()) {
			errand.setBusinessRelated(null);
		}
		if (!include.isEscalationEmail()) {
			errand.setEscalationEmail(null);
		}
		if (!include.isContactReasonDescription()) {
			errand.setContactReasonDescription(null);
		}

		errand.setId(null);
		errand.setErrandNumber(null);
		errand.setResolution(null);
		errand.setSuspension(null);

		return errand;
	}

	/**
	 * Builds a map of the field mappings that were applied when creating the target errand.
	 */
	public static Map<String, String> buildAppliedMappings(final HandoverErrandRequest request) {
		final var applied = new LinkedHashMap<String, String>();
		final var mapping = request.getMapping();
		ofNullable(mapping.getStatus()).ifPresent(v -> applied.put("status", v));
		ofNullable(mapping.getClassification()).ifPresent(c -> {
			ofNullable(c.getCategory()).ifPresent(v -> applied.put("classification.category", v));
			ofNullable(c.getType()).ifPresent(v -> applied.put("classification.type", v));
		});
		ofNullable(mapping.getLabels()).ifPresent(v -> applied.put("labels", v.toString()));
		ofNullable(mapping.getContactReason()).ifPresent(v -> applied.put("contactReason", v));
		ofNullable(mapping.getChannel()).ifPresent(v -> applied.put("channel", v));
		return applied;
	}

	/**
	 * Builds a list of non-fatal warnings for fields that were copied but may not be valid in the target namespace.
	 */
	public static List<String> buildWarnings(final ErrandEntity source, final HandoverErrandRequest request) {
		final var warnings = new ArrayList<String>();
		final var include = ofNullable(request.getInclude()).orElseGet(HandoverInclude::create);

		if (include.isParameters() && nonNull(source.getParameters()) && !source.getParameters().isEmpty()) {
			warnings.add("Parameters were copied but may contain namespace-specific schemaIds that are not valid in the target namespace");
		}
		if (include.isJsonParameters() && nonNull(source.getJsonParameters()) && !source.getJsonParameters().isEmpty()) {
			warnings.add("JSON parameters were copied but may contain namespace-specific schemaIds that are not valid in the target namespace");
		}
		if (include.isStakeholders() && nonNull(source.getStakeholders()) && !source.getStakeholders().isEmpty()) {
			warnings.add("Stakeholders were copied but their roles may not exist in the target namespace");
		}
		if (request.getMapping().getChannel() == null && nonNull(source.getChannel())) {
			warnings.add("Channel was copied from source errand but may not be valid in the target namespace");
		}
		return warnings;
	}

	private static List<ErrandLabel> toErrandLabels(final List<String> labelIds) {
		return ofNullable(labelIds).orElse(emptyList()).stream()
			.map(id -> ErrandLabel.create().withId(id))
			.toList();
	}

	/**
	 * Decodes a pipe-delimited warnings string stored in the idempotency table back to a list.
	 */
	public static List<String> decodeWarnings(final String encoded) {
		if (encoded == null || encoded.isBlank()) {
			return List.of();
		}
		return List.of(encoded.split("\\|~\\|", -1));
	}

	/**
	 * Encodes a warnings list as a pipe-delimited string for storage in the idempotency table.
	 */
	public static String encodeWarnings(final List<String> warnings) {
		return Optional.ofNullable(warnings).orElse(List.of()).stream()
			.map(w -> w.replace("|~|", " "))
			.reduce((a, b) -> a + "|~|" + b)
			.orElse("");
	}
}
