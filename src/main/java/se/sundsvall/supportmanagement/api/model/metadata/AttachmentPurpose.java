package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * A purpose an attachment of the errand may be given, registered per namespace.
 * <p>
 * The values recommended for the model - SUPPORTING, RESPONSE, DECISION, APPENDIX and PROTOCOL - are not seeded. A
 * namespace registers the ones it uses.
 */
@Schema(description = "AttachmentPurpose model")
public class AttachmentPurpose {

	@Schema(description = "AttachmentPurpose ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Name for the attachment purpose. Used as key", examples = "RESPONSE")
	@NotBlank
	private String name;

	@Schema(description = "Display name for the attachment purpose", examples = "Inkommen handling", types = {
		"string", "null"
	})
	private String displayName;

	@Schema(description = "Sort order for the attachment purpose", examples = "1", types = {
		"integer", "null"
	})
	private Integer sortOrder;

	@Schema(description = "Indicates if the attachment purpose is deprecated", defaultValue = "false", examples = "true")
	private Boolean deprecated;

	@Schema(description = "Timestamp when the attachment purpose was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the attachment purpose was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	public static AttachmentPurpose create() {
		return new AttachmentPurpose();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public AttachmentPurpose withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public AttachmentPurpose withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public AttachmentPurpose withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public AttachmentPurpose withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public AttachmentPurpose withDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public AttachmentPurpose withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public AttachmentPurpose withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, displayName, sortOrder, deprecated, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final AttachmentPurpose other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(name, other.name)
			&& Objects.equals(displayName, other.displayName)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& Objects.equals(deprecated, other.deprecated)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "AttachmentPurpose{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", sortOrder=" + sortOrder +
			", deprecated=" + deprecated +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
