package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.ContactChannel;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ErrandAction;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.api.model.errand.ErrandPhase;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.api.model.errand.Suspension;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandPhaseEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static java.util.Collections.emptyList;
import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.mergeParameters;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toErrandParameterEntityList;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameterList;
import static se.sundsvall.supportmanagement.service.mapper.StakeholderParameterMapper.toParameterList;
import static se.sundsvall.supportmanagement.service.mapper.StakeholderParameterMapper.toStakeholderParameterEntityList;

public final class ErrandMapper {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandMapper.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE = "case";
	private static final String REFERRED_FROM_RESOURCE_IDENTIFIER_SERVICE = "support-management";

	private ErrandMapper() {}

	public static ErrandEntity toErrandEntity(final String namespace, final String municipalityId, final Errand errand) {
		if (anyNull(namespace, municipalityId, errand)) {
			return null;
		}
		final var errandEntity = ErrandEntity.create();
		return errandEntity
			.withAssignedGroupId(errand.getAssignedGroupId())
			.withAssignedUserId(errand.getAssignedUserId())
			.withAttachments(new ArrayList<>())
			.withCategory(ofNullable(errand.getClassification()).map(Classification::getCategory).orElse(null))
			.withType(ofNullable(errand.getClassification()).map(Classification::getType).orElse(null))
			.withDescription(errand.getDescription())
			.withChannel(errand.getChannel())
			.withEscalationEmail(errand.getEscalationEmail())
			.withExternalTags(toExternalTag(errand.getExternalTags()))
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withPriority(errand.getPriority().name())
			.withReporterUserId(errand.getReporterUserId())
			.withResolution(errand.getResolution())
			.withStakeholders(toStakeholderEntities(errandEntity, errand.getStakeholders()))
			.withStatus(errand.getStatus())
			.withTitle(errand.getTitle())
			.withErrandNumber(errand.getErrandNumber())
			.withSuspendedFrom(Optional.ofNullable(errand.getSuspension()).map(Suspension::getSuspendedFrom).orElse(null))
			.withSuspendedTo(Optional.ofNullable(errand.getSuspension()).map(Suspension::getSuspendedTo).orElse(null))
			.withBusinessRelated(errand.getBusinessRelated())
			.withParameters(toErrandParameterEntityList(errand.getParameters(), errandEntity))
			.withJsonParameters(toJsonParameterEntities(errand.getJsonParameters(), errandEntity))
			.withLabels(toErrandLabelEmbeddables(errand.getLabels()));
	}

	public static ErrandEntity updateEntity(final ErrandEntity entity, final Errand errand) {
		if (isNull(errand)) {
			return entity;
		}

		ofNullable(errand.getAssignedGroupId()).ifPresent(value -> entity.setAssignedGroupId(isEmpty(value) ? null : value));
		ofNullable(errand.getAssignedUserId()).ifPresent(value -> entity.setAssignedUserId(isEmpty(value) ? null : value));
		ofNullable(errand.getClassification()).ifPresent(value -> {
			entity.setCategory(value.getCategory());
			entity.setType(value.getType());
		});
		ofNullable(errand.getSuspension()).ifPresent(value -> {
			entity.setSuspendedFrom(value.getSuspendedFrom());
			entity.setSuspendedTo(value.getSuspendedTo());
		});
		ofNullable(errand.getStakeholders()).ifPresent(value -> updateStakeholders(entity, value));
		ofNullable(errand.getExternalTags()).ifPresent(value -> entity.setExternalTags(toExternalTag(value)));
		ofNullable(errand.getPriority()).ifPresent(value -> entity.setPriority(value.name()));
		ofNullable(errand.getStatus()).ifPresent(entity::setStatus);
		ofNullable(errand.getTitle()).ifPresent(entity::setTitle);
		ofNullable(errand.getResolution()).ifPresent(value -> entity.setResolution(isEmpty(value) ? null : value));
		ofNullable(errand.getDescription()).ifPresent(value -> entity.setDescription(isEmpty(value) ? null : value));
		ofNullable(errand.getChannel()).ifPresent(value -> entity.setChannel(isEmpty(value) ? null : value));
		ofNullable(errand.getContactReasonDescription()).ifPresent(value -> entity.setContactReasonDescription(isEmpty(value) ? null : value));
		ofNullable(errand.getEscalationEmail()).ifPresent(value -> entity.setEscalationEmail(isEmpty(value) ? null : value));
		ofNullable(errand.getBusinessRelated()).ifPresent(entity::setBusinessRelated);
		ofNullable(errand.getParameters()).ifPresent(value -> updateParameters(entity, value));
		ofNullable(errand.getJsonParameters()).ifPresent(value -> updateJsonParameters(entity, value));
		ofNullable(errand.getLabels()).ifPresent(value -> entity.setLabels(toErrandLabelEmbeddables(value)));
		return entity;
	}

	public static List<ErrandLabelEmbeddable> toErrandLabelEmbeddables(final List<ErrandLabel> errandLabels) {
		return ofNullable(errandLabels).orElse(emptyList()).stream()
			.map(errandLabel -> ErrandLabelEmbeddable.create()
				.withMetadataLabelId(errandLabel.getId()))
			.distinct()
			.collect(toCollection(ArrayList::new));
	}

	private static void updateParameters(final ErrandEntity entity, final List<Parameter> parameters) {
		mergeParameters(entity, parameters);
	}

	private static void updateJsonParameters(final ErrandEntity entity, final List<JsonParameter> jsonParameters) {
		if (entity.getJsonParameters() == null) {
			entity.setJsonParameters(new ArrayList<>());
		}
		final var existing = entity.getJsonParameters();
		final var incomingByKey = jsonParameters.stream().collect(toMap(JsonParameter::getKey, identity(), (a, b) -> b));
		final var existingByKey = existing.stream().collect(toMap(JsonParameterEntity::getKey, identity()));

		existing.removeIf(e -> !incomingByKey.containsKey(e.getKey()));
		existing.forEach(e -> {
			final var incoming = incomingByKey.get(e.getKey());
			e.setSchemaId(incoming.getSchemaId());
			e.setValue(toJsonString(incoming.getValue()));
		});
		jsonParameters.stream()
			.filter(p -> !existingByKey.containsKey(p.getKey()))
			.map(p -> JsonParameterEntity.create()
				.withErrandEntity(entity)
				.withKey(p.getKey())
				.withSchemaId(p.getSchemaId())
				.withValue(toJsonString(p.getValue())))
			.forEach(existing::add);
	}

	private static void updateStakeholders(final ErrandEntity entity, final List<Stakeholder> stakeholders) {
		ofNullable(entity.getStakeholders()).ifPresentOrElse(List::clear, () -> entity.setStakeholders(new ArrayList<>()));
		entity.getStakeholders().addAll(toStakeholderEntities(entity, stakeholders));
	}

	private static List<DbExternalTag> toExternalTag(final List<ExternalTag> tags) {
		return ofNullable(tags).orElse(emptyList()).stream()
			.map(ErrandMapper::toExternalTagEntity)
			.collect(toCollection(ArrayList::new));
	}

	private static DbExternalTag toExternalTagEntity(final ExternalTag tag) {
		return DbExternalTag.create()
			.withKey(tag.getKey())
			.withValue(tag.getValue());
	}

	public static List<Errand> toErrands(final List<ErrandEntity> entities) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(ErrandMapper::toErrand)
			.toList();
	}

	/**
	 * Maps a single field of an errand. Keyed fields limit themselves to sent in keys, an empty set meaning the whole
	 * collection.
	 */
	@FunctionalInterface
	private interface FieldMapper {
		void map(Errand errand, ErrandEntity entity, Set<String> keys);
	}

	/**
	 * One entry per {@link ErrandField}, so exposing a new field is a matter of adding a constant and an entry here.
	 * {@code ErrandMapperTest} asserts that the two stay in step.
	 */
	private static final Map<ErrandField, FieldMapper> FIELD_MAPPERS = new EnumMap<>(Map.ofEntries(
		entry(ErrandField.ID, (errand, e, _) -> errand.setId(e.getId())),
		entry(ErrandField.ERRAND_NUMBER, (errand, e, _) -> errand.setErrandNumber(e.getErrandNumber())),
		entry(ErrandField.TITLE, (errand, e, _) -> errand.setTitle(e.getTitle())),
		entry(ErrandField.STATUS, (errand, e, _) -> errand.setStatus(e.getStatus())),
		entry(ErrandField.RESOLUTION, (errand, e, _) -> errand.setResolution(e.getResolution())),
		entry(ErrandField.CHANNEL, (errand, e, _) -> errand.setChannel(e.getChannel())),
		entry(ErrandField.CREATED, (errand, e, _) -> errand.setCreated(e.getCreated())),
		entry(ErrandField.MODIFIED, (errand, e, _) -> errand.setModified(e.getModified())),
		entry(ErrandField.TOUCHED, (errand, e, _) -> errand.setTouched(e.getTouched())),
		entry(ErrandField.PRIORITY, (errand, e, _) -> errand.setPriority(Priority.valueOf(e.getPriority()))),
		entry(ErrandField.DESCRIPTION, (errand, e, _) -> errand.setDescription(e.getDescription())),
		entry(ErrandField.CLASSIFICATION, (errand, e, _) -> errand.setClassification(Classification.create().withCategory(e.getCategory()).withType(e.getType()))),
		entry(ErrandField.REPORTER_USER_ID, (errand, e, _) -> errand.setReporterUserId(e.getReporterUserId())),
		entry(ErrandField.ASSIGNED_USER_ID, (errand, e, _) -> errand.setAssignedUserId(e.getAssignedUserId())),
		entry(ErrandField.ASSIGNED_GROUP_ID, (errand, e, _) -> errand.setAssignedGroupId(e.getAssignedGroupId())),
		entry(ErrandField.BUSINESS_RELATED, (errand, e, _) -> errand.setBusinessRelated(e.getBusinessRelated())),
		entry(ErrandField.SUSPENSION, (errand, e, _) -> errand.setSuspension(Suspension.create().withSuspendedFrom(e.getSuspendedFrom()).withSuspendedTo(e.getSuspendedTo()))),
		entry(ErrandField.CONTACT_REASON, (errand, e, _) -> errand.setContactReason(ofNullable(e.getContactReason()).map(ContactReasonEntity::getReason).orElse(null))),
		entry(ErrandField.CONTACT_REASON_DESCRIPTION, (errand, e, _) -> errand.setContactReasonDescription(e.getContactReasonDescription())),
		entry(ErrandField.ESCALATION_EMAIL, (errand, e, _) -> errand.setEscalationEmail(e.getEscalationEmail())),
		entry(ErrandField.LABELS, (errand, e, _) -> errand.setLabels(toErrandLabels(e.getLabels()))),
		entry(ErrandField.STAKEHOLDERS, (errand, e, _) -> errand.setStakeholders(toStakeholders(e.getStakeholders()))),
		entry(ErrandField.ACTIVE_NOTIFICATIONS, (errand, e, _) -> errand.setActiveNotifications(toActiveNotifications(e.getNotifications()))),
		entry(ErrandField.VERSION, (errand, e, _) -> errand.setVersion(e.getVersion())),
		entry(ErrandField.PARAMETERS, (errand, e, keys) -> errand.setParameters(filterByKey(toParameterList(e.getParameters()), Parameter::getKey, keys))),
		entry(ErrandField.JSON_PARAMETERS, (errand, e, keys) -> errand.setJsonParameters(filterByKey(toJsonParameters(e.getJsonParameters()), JsonParameter::getKey, keys))),
		entry(ErrandField.EXTERNAL_TAGS, (errand, e, keys) -> errand.setExternalTags(filterByKey(toExternalTags(e.getExternalTags()), ExternalTag::getKey, keys)))));

	private static <T> List<T> filterByKey(final List<T> values, final Function<T, String> keyExtractor, final Set<String> keys) {
		if (isNull(keys) || keys.isEmpty()) {
			return values;
		}
		return ofNullable(values).orElse(emptyList()).stream()
			.filter(value -> keys.contains(keyExtractor.apply(value)))
			.toList();
	}

	/**
	 * Maps errands according to the fields the requesting user holds through their roles. A user holding no configured
	 * role, and every user when role based mapping is inactive, receives the full errand.
	 *
	 * @param  entities         errands to map
	 * @param  roleBasedMapping true if the namespace maps errands per role
	 * @param  fieldResolver    resolver of the fields, and the keys to limit them to, the user may see per errand
	 * @return                  mapped errands
	 */
	public static List<Errand> toErrandsWithAccessControl(final List<ErrandEntity> entities, final boolean roleBasedMapping, final Function<ErrandEntity, Map<ErrandField, Set<String>>> fieldResolver) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(entity -> toErrandWithAccessControl(entity, roleBasedMapping, fieldResolver))
			.toList();
	}

	/**
	 * Maps an errand according to the fields the requesting user holds through their roles. A user holding no configured
	 * role, and every user when role based mapping is inactive, receives the full errand.
	 *
	 * @param  entity           errand to map
	 * @param  roleBasedMapping true if the namespace maps errands per role
	 * @param  fieldResolver    resolver of the fields, and the keys to limit them to, the user may see for the errand
	 * @return                  mapped errand
	 */
	public static Errand toErrandWithAccessControl(final ErrandEntity entity, final boolean roleBasedMapping, final Function<ErrandEntity, Map<ErrandField, Set<String>>> fieldResolver) {
		if (isNull(entity) || !roleBasedMapping) {
			return toErrand(entity);
		}

		final var fields = fieldResolver.apply(entity);
		return isNull(fields) || fields.isEmpty() ? toErrand(entity) : toRoleMappedErrand(entity, fields);
	}

	private static Errand toRoleMappedErrand(final ErrandEntity entity, final Map<ErrandField, Set<String>> fields) {
		final var errand = Errand.create();
		fields.forEach((field, keys) -> FIELD_MAPPERS.get(field).map(errand, entity, keys));
		return errand;
	}

	static Map<ErrandField, FieldMapper> fieldMappers() {
		return FIELD_MAPPERS;
	}

	public static Errand toErrand(final ErrandEntity entity) {
		return ofNullable(entity)
			.map(e -> Errand.create()
				.withAssignedGroupId(e.getAssignedGroupId())
				.withAssignedUserId(e.getAssignedUserId())
				.withClassification(Classification.create().withCategory(e.getCategory()).withType(e.getType()))
				.withCreated(e.getCreated())
				.withStakeholders(toStakeholders(e.getStakeholders()))
				.withExternalTags(toExternalTags(e.getExternalTags()))
				.withId(e.getId())
				.withErrandNumber(e.getErrandNumber())
				.withModified(e.getModified())
				.withPriority(Priority.valueOf(e.getPriority()))
				.withReporterUserId(e.getReporterUserId())
				.withStatus(e.getStatus())
				.withTitle(e.getTitle())
				.withTouched(e.getTouched())
				.withResolution(e.getResolution())
				.withDescription(e.getDescription())
				.withChannel(e.getChannel())
				.withSuspension(Suspension.create().withSuspendedFrom(e.getSuspendedFrom()).withSuspendedTo(e.getSuspendedTo()))
				.withBusinessRelated(e.getBusinessRelated())
				.withParameters(toParameterList(e.getParameters()))
				.withJsonParameters(toJsonParameters(e.getJsonParameters()))
				.withContactReason(ofNullable(e.getContactReason()).map(ContactReasonEntity::getReason).orElse(null))
				.withContactReasonDescription(e.getContactReasonDescription())
				.withEscalationEmail(e.getEscalationEmail())
				.withLabels(toErrandLabels(e.getLabels()))
				.withPhases(toErrandPhases(e.getPhases()))
				.withActiveNotifications(toActiveNotifications(e.getNotifications()))
				.withActions(toErrandActions(e.getActions()))
				.withVersion(e.getVersion()))
			.orElse(null);
	}

	public static List<ErrandLabel> toErrandLabels(final List<ErrandLabelEmbeddable> errandLabelEmbeddables) {
		return ofNullable(errandLabelEmbeddables).orElse(emptyList())
			.stream()
			.map(errandLabelEmbeddable -> ErrandLabel.create()
				.withClassification(nonNull(errandLabelEmbeddable.getMetadataLabel()) ? errandLabelEmbeddable.getMetadataLabel().getClassification() : null)
				.withDisplayName(nonNull(errandLabelEmbeddable.getMetadataLabel()) ? errandLabelEmbeddable.getMetadataLabel().getDisplayName() : null)
				.withId(errandLabelEmbeddable.getMetadataLabelId())
				.withResourceName(nonNull(errandLabelEmbeddable.getMetadataLabel()) ? errandLabelEmbeddable.getMetadataLabel().getResourceName() : null)
				.withResourcePath(nonNull(errandLabelEmbeddable.getMetadataLabel()) ? errandLabelEmbeddable.getMetadataLabel().getResourcePath() : null))
			.toList();
	}

	private static List<Stakeholder> toStakeholders(final List<StakeholderEntity> stakeholderEntities) {
		return ofNullable(stakeholderEntities)
			.map(s -> s.stream()
				.map(stakeholderEntity -> Stakeholder.create()
					.withExternalId(stakeholderEntity.getExternalId())
					.withExternalIdType(stakeholderEntity.getExternalIdType())
					.withCity(stakeholderEntity.getCity())
					.withOrganizationName(stakeholderEntity.getOrganizationName())
					.withRole(stakeholderEntity.getRole())
					.withFirstName(stakeholderEntity.getFirstName())
					.withLastName(stakeholderEntity.getLastName())
					.withAddress(stakeholderEntity.getAddress())
					.withCareOf(stakeholderEntity.getCareOf())
					.withZipCode(stakeholderEntity.getZipCode())
					.withCountry(stakeholderEntity.getCountry())
					.withParameters(toParameterList(stakeholderEntity.getParameters()))
					.withContactChannels(toContactChannels(stakeholderEntity.getContactChannels())))
				.toList())
			.orElse(emptyList());
	}

	private static List<StakeholderEntity> toStakeholderEntities(final ErrandEntity errandEntity, final List<Stakeholder> stakeholders) {
		return new ArrayList<>(ofNullable(stakeholders)
			.map(s -> s.stream()
				.map(stakeholder -> {
					final var stakeholderEntity = StakeholderEntity.create()
						.withErrandEntity(errandEntity)
						.withExternalId(stakeholder.getExternalId())
						.withExternalIdType(stakeholder.getExternalIdType())
						.withCity(stakeholder.getCity())
						.withOrganizationName(stakeholder.getOrganizationName())
						.withRole(stakeholder.getRole())
						.withFirstName(stakeholder.getFirstName())
						.withLastName(stakeholder.getLastName())
						.withAddress(stakeholder.getAddress())
						.withCareOf(stakeholder.getCareOf())
						.withZipCode(stakeholder.getZipCode())
						.withCountry(stakeholder.getCountry())
						.withContactChannels(toContactChannelEntities(stakeholder.getContactChannels()));

					return stakeholderEntity
						.withParameters(toStakeholderParameterEntityList(stakeholder.getParameters(), stakeholderEntity));
				})
				.toList())
			.orElse(emptyList()));
	}

	private static List<ContactChannelEntity> toContactChannelEntities(final List<ContactChannel> contactChannels) {
		return ofNullable(contactChannels).orElse(emptyList()).stream()
			.map(contactChannel -> ContactChannelEntity.create()
				.withType(contactChannel.getType())
				.withValue(contactChannel.getValue()))
			.collect(toCollection(ArrayList::new));
	}

	private static List<ContactChannel> toContactChannels(final List<ContactChannelEntity> contactChannelEntities) {
		return ofNullable(contactChannelEntities)
			.map(ch -> ch.stream()
				.map(contactChannelEntity -> ContactChannel.create()
					.withType(contactChannelEntity.getType())
					.withValue(contactChannelEntity.getValue()))
				.toList())
			.orElse(emptyList());
	}

	private static List<ExternalTag> toExternalTags(final List<DbExternalTag> entities) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(ErrandMapper::toExternalTag)
			.toList();
	}

	private static ExternalTag toExternalTag(final DbExternalTag entity) {
		return ExternalTag.create()
			.withKey(entity.getKey())
			.withValue(entity.getValue());
	}

	private static List<Notification> toActiveNotifications(final List<NotificationEntity> entities) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.filter(notification -> !notification.isGlobalAcknowledged() || !notification.isAcknowledged())
			.map(NotificationMapper::toNotification)
			.toList();
	}

	private static List<ErrandPhase> toErrandPhases(final List<ErrandPhaseEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(entity -> {
				final var phase = entity.getPhaseEntity();
				return ErrandPhase.create()
					.withPhaseId(nonNull(phase) ? phase.getId() : null)
					.withName(nonNull(phase) ? phase.getName() : null)
					.withDisplayName(nonNull(phase) ? phase.getDisplayName() : null)
					.withStarted(entity.getStarted())
					.withEnded(entity.getEnded());
			})
			.toList();
	}

	private static List<ErrandAction> toErrandActions(final List<ErrandActionEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(entity -> ErrandAction.create()
				.withId(entity.getId())
				.withActionName(ofNullable(entity.getActionConfigEntity()).map(ActionConfigEntity::getName).orElse(null))
				.withExecuteAfter(entity.getExecuteAfter())
				.withActionConfigId(ofNullable(entity.getActionConfigEntity()).map(ActionConfigEntity::getId).orElse(null))
				.withDisplayValue(ofNullable(entity.getActionConfigEntity()).map(ActionConfigEntity::getDisplayValue).orElse(null)))
			.toList();
	}

	public static Relation toReferredFromRelation(final String namespace, final se.sundsvall.dept44.support.Relation referredFrom, final String newErrandId) {
		if (Objects.isNull(referredFrom) || Objects.isNull(referredFrom.getSource())) {
			return null;
		}
		return new Relation()
			.type(referredFrom.getType())
			.source(new ResourceIdentifier()
				.resourceId(referredFrom.getSource().getResourceId())
				.type(REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE)
				.service(referredFrom.getSource().getService())
				.namespace(referredFrom.getSource().getNamespace()))
			.target(new ResourceIdentifier()
				.resourceId(newErrandId)
				.type(REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE)
				.service(REFERRED_FROM_RESOURCE_IDENTIFIER_SERVICE)
				.namespace(namespace));
	}

	private static List<JsonParameterEntity> toJsonParameterEntities(final List<JsonParameter> jsonParameters, final ErrandEntity errandEntity) {
		return ofNullable(jsonParameters).orElse(emptyList()).stream()
			.map(param -> JsonParameterEntity.create()
				.withErrandEntity(errandEntity)
				.withKey(param.getKey())
				.withSchemaId(param.getSchemaId())
				.withValue(toJsonString(param.getValue())))
			.collect(toCollection(ArrayList::new));
	}

	public static JsonParameter toJsonParameter(final JsonParameterEntity entity) {
		return JsonParameter.create()
			.withKey(entity.getKey())
			.withSchemaId(entity.getSchemaId())
			.withValue(toJsonNode(entity.getValue()))
			.withVersion(entity.getVersion());
	}

	private static List<JsonParameter> toJsonParameters(final List<JsonParameterEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandMapper::toJsonParameter)
			.toList();
	}

	public static String toJsonString(final JsonNode jsonNode) {
		if (isNull(jsonNode)) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(jsonNode);
		} catch (final JacksonException e) {
			LOG.warn("Failed to convert JsonNode to String", e);
			return null;
		}
	}

	private static JsonNode toJsonNode(final String jsonString) {
		if (isNull(jsonString)) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readTree(jsonString);
		} catch (final JacksonException e) {
			LOG.warn("Failed to convert String to JsonNode", e);
			return null;
		}
	}
}
