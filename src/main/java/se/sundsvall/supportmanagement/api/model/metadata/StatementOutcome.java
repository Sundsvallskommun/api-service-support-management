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
 * An outcome the response to a statement may have, registered per namespace.
 */
@Schema(description = "StatementOutcome model")
public class StatementOutcome {

	@Schema(description = "StatementOutcome ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Name for the statement outcome. Used as key", examples = "SUPPORTS")
	@NotBlank
	private String name;

	@Schema(description = "Display name for the statement outcome", examples = "Tillstyrker", types = {
		"string", "null"
	})
	private String displayName;

	@Schema(description = "Sort order for the statement outcome", examples = "1", types = {
		"integer", "null"
	})
	private Integer sortOrder;

	@Schema(description = "Indicates if the outcome means that the counterparty responded. A statement completed with such an outcome needs respondedAt, "
		+ "one completed with an outcome that does not - such as no response within the deadline - does not", defaultValue = "true", examples = "true")
	private Boolean responded;

	@Schema(description = "Indicates if the statement outcome is deprecated", defaultValue = "false", examples = "true")
	private Boolean deprecated;

	@Schema(description = "Timestamp when the statement outcome was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the statement outcome was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Null
	private OffsetDateTime modified;

	public static StatementOutcome create() {
		return new StatementOutcome();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public StatementOutcome withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public StatementOutcome withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public StatementOutcome withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public StatementOutcome withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public Boolean getResponded() {
		return responded;
	}

	public void setResponded(final Boolean responded) {
		this.responded = responded;
	}

	public StatementOutcome withResponded(final Boolean responded) {
		this.responded = responded;
		return this;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public StatementOutcome withDeprecated(final Boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public StatementOutcome withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public StatementOutcome withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, displayName, sortOrder, responded, deprecated, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final StatementOutcome other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(name, other.name)
			&& Objects.equals(displayName, other.displayName)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& Objects.equals(responded, other.responded)
			&& Objects.equals(deprecated, other.deprecated)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "StatementOutcome{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", sortOrder=" + sortOrder +
			", responded=" + responded +
			", deprecated=" + deprecated +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
