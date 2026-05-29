package se.sundsvall.supportmanagement.api.model.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

public class SubscriberNotification {

	@Schema(description = "Unique identifier for the notification", example = "123e4567-e89b-12d3-a456-426614174000", accessMode = READ_ONLY)
	private String id;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the notification was created", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the notification was last modified", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	@Schema(description = "Identifier type of the notification owner", example = "adAccount", accessMode = READ_ONLY)
	private String identifierType;

	@Schema(description = "Identifier value of the notification owner", example = "joe01doe", accessMode = READ_ONLY)
	private String identifierValue;

	@Schema(description = "ID of the errand this notification relates to", example = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	private String errandId;

	@Schema(description = "Number of the errand this notification relates to", example = "PRH-2022-000001", accessMode = READ_ONLY)
	private String errandNumber;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the notification expires", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime expires;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the notification was acknowledged, null if not yet acknowledged", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime acknowledged;

	public static SubscriberNotification create() {
		return new SubscriberNotification();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public SubscriberNotification withId(final String id) {
		this.id = id;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriberNotification withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public SubscriberNotification withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public String getIdentifierType() {
		return identifierType;
	}

	public void setIdentifierType(final String identifierType) {
		this.identifierType = identifierType;
	}

	public SubscriberNotification withIdentifierType(final String identifierType) {
		this.identifierType = identifierType;
		return this;
	}

	public String getIdentifierValue() {
		return identifierValue;
	}

	public void setIdentifierValue(final String identifierValue) {
		this.identifierValue = identifierValue;
	}

	public SubscriberNotification withIdentifierValue(final String identifierValue) {
		this.identifierValue = identifierValue;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public SubscriberNotification withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public SubscriberNotification withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public OffsetDateTime getExpires() {
		return expires;
	}

	public void setExpires(final OffsetDateTime expires) {
		this.expires = expires;
	}

	public SubscriberNotification withExpires(final OffsetDateTime expires) {
		this.expires = expires;
		return this;
	}

	public OffsetDateTime getAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(final OffsetDateTime acknowledged) {
		this.acknowledged = acknowledged;
	}

	public SubscriberNotification withAcknowledged(final OffsetDateTime acknowledged) {
		this.acknowledged = acknowledged;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final SubscriberNotification that = (SubscriberNotification) o;
		return Objects.equals(id, that.id)
			&& Objects.equals(created, that.created)
			&& Objects.equals(modified, that.modified)
			&& Objects.equals(identifierType, that.identifierType)
			&& Objects.equals(identifierValue, that.identifierValue)
			&& Objects.equals(errandId, that.errandId)
			&& Objects.equals(errandNumber, that.errandNumber)
			&& Objects.equals(expires, that.expires)
			&& Objects.equals(acknowledged, that.acknowledged);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, created, modified, identifierType, identifierValue, errandId, errandNumber, expires, acknowledged);
	}

	@Override
	public String toString() {
		return "SubscriberNotification{" +
			"id='" + id + '\'' +
			", created=" + created +
			", modified=" + modified +
			", identifierType='" + identifierType + '\'' +
			", identifierValue='" + identifierValue + '\'' +
			", errandId='" + errandId + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", expires=" + expires +
			", acknowledged=" + acknowledged +
			'}';
	}
}
