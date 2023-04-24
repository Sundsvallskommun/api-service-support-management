package se.sundsvall.supportmanagement.api.model.revision;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Revision model", accessMode = READ_ONLY)
public class Revision {

	@Schema(description = "Unique id for the revision", example = "391e97b7-2e78-42e2-9a60-fe49fbfa94f1")
	private String id;

	@Schema(description = "Unique id for the entity connected to the revision", example = "3af4844d-a75f-4e25-a2a0-355eb642dd2d")
	private String entityId;

	@Schema(description = "Type of entity for the revision", example = "ErrandEntity")
	private String entityType;

	@Schema(description = "Version of the revision", example = "1")
	private Integer version;

	@Schema(description = "Timestamp when the revision was created", example = "2000-10-31T01:30:00.000+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	public static Revision create() {
		return new Revision();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Revision withId(final String id) {
		this.id = id;
		return this;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(final String entityId) {
		this.entityId = entityId;
	}

	public Revision withEntityId(final String entityId) {
		this.entityId = entityId;
		return this;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(final String entityType) {
		this.entityType = entityType;
	}

	public Revision withEntityType(final String entityType) {
		this.entityType = entityType;
		return this;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(final Integer version) {
		this.version = version;
	}

	public Revision withVersion(final Integer version) {
		this.version = version;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Revision withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, entityId, entityType, id, version);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Revision other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(entityId, other.entityId) && Objects.equals(entityType, other.entityType) && Objects.equals(id, other.id) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Revision [id=")
			.append(id).append(", entityId=").append(entityId)
			.append(", entityType=").append(entityType)
			.append(", version=").append(version)
			.append(", created=").append(created)
			.append("]");
		return builder.toString();
	}
}
