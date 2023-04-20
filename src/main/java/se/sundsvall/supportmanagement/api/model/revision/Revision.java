package se.sundsvall.supportmanagement.api.model.revision;

import java.time.OffsetDateTime;
import java.util.Objects;

public class Revision {

	private String id;

	private String entityId;

	private String entityType;

	private Integer version;

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
