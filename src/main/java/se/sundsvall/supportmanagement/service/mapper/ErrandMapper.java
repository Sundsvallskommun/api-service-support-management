package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toErrandParameterEntityList;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameterList;
import static se.sundsvall.supportmanagement.service.mapper.StakeholderParameterMapper.toParameterList;
import static se.sundsvall.supportmanagement.service.mapper.StakeholderParameterMapper.toStakeholderParameterEntityList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.ContactChannel;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.api.model.errand.Suspension;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

public final class ErrandMapper {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandMapper.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String REFERRED_FROM_RELATION_TYPE = "REFERRED_FROM";
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
			.withAttachments(emptyList())
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
		ofNullable(errand.getBusinessRelated()).ifPresent(value -> entity.setBusinessRelated(value));
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
		ofNullable(entity.getParameters()).ifPresentOrElse(List::clear, () -> entity.setParameters(new ArrayList<>()));
		entity.getParameters().addAll(toErrandParameterEntityList(parameters, entity));
	}

	private static void updateJsonParameters(final ErrandEntity entity, final List<JsonParameter> jsonParameters) {
		ofNullable(entity.getJsonParameters()).ifPresentOrElse(List::clear, () -> entity.setJsonParameters(new ArrayList<>()));
		entity.getJsonParameters().addAll(toJsonParameterEntities(jsonParameters, entity));
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

	public static List<Errand> toErrandsWithAccessControl(final List<ErrandEntity> entities, final Predicate<ErrandEntity> mapLimited) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(errandEntity -> toErrandWithAccessControl(errandEntity, mapLimited))
			.toList();
	}

	public static Errand toErrandWithAccessControl(final ErrandEntity entity, final Predicate<ErrandEntity> mapLimited) {
		return mapLimited.test(entity) ? toLimitedErrand(entity) : toErrand(entity);
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
				.withActiveNotifications(toActiveNotifications(e.getNotifications())))
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

	public static Errand toLimitedErrand(final ErrandEntity entity) {
		return ofNullable(entity)
			.map(e -> Errand.create()
				.withId(e.getId())
				.withCreated(e.getCreated())
				.withErrandNumber(e.getErrandNumber())
				.withModified(e.getModified())
				.withStatus(e.getStatus())
				.withTitle(e.getTitle())
				.withTouched(e.getTouched())
				.withResolution(e.getResolution())
				.withChannel(e.getChannel()))
			.orElse(null);
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

	public static Relation toReferredFromRelation(final String namespace, final String referredFromErrandId, final String newErrandId) {
		return new Relation()
			.type(REFERRED_FROM_RELATION_TYPE)
			.source(new ResourceIdentifier()
				.resourceId(referredFromErrandId)
				.type(REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE)
				.service(REFERRED_FROM_RESOURCE_IDENTIFIER_SERVICE)
				.namespace(namespace))
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

	private static List<JsonParameter> toJsonParameters(final List<JsonParameterEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(entity -> JsonParameter.create()
				.withKey(entity.getKey())
				.withSchemaId(entity.getSchemaId())
				.withValue(toJsonNode(entity.getValue())))
			.toList();
	}

	private static String toJsonString(final JsonNode jsonNode) {
		if (isNull(jsonNode)) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(jsonNode);
		} catch (final JsonProcessingException e) {
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
		} catch (final JsonProcessingException e) {
			LOG.warn("Failed to convert String to JsonNode", e);
			return null;
		}
	}
}
