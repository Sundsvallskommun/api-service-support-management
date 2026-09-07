package se.sundsvall.supportmanagement.api.model.errand;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The required fields are demanded of every measure that is not being patched on its own resource.
 * <p>
 * Patching a single measure says nothing about the fields it leaves out, so the OnUpdate group deliberately omits them.
 * Everywhere else - creating a measure, and carrying measures on the errand - a measure is expected to be complete,
 * which is what stops one from being persisted blank. The accept value is checked wherever it is supplied, since an
 * unknown one would otherwise reach the mapper and surface as a 500 rather than the bad request it is.
 */
@Schema(description = "Measure model")
public class Measure {

	/** The fields a write can supply. */
	public enum Field {
		RESPONSIBLE_USER, TYPE, PLANNED_START, PLANNED_COMPLETE, EXECUTED, ADDED_BY_USER, ADDED_BY_ROLE,
		GOAL, DESCRIPTION, ACCEPT, ACCEPT_MOTIVATION, REWORK_GOAL, REWORK_DESCRIPTION
	}

	/**
	 * The fields a write supplied, including the ones it set to null, which is what lets a patch clear a field rather
	 * than leave it alone. Every setter records its field, and Jackson calls the setter for an explicit null. Never part
	 * of the JSON representation, and deliberately left out of equals, hashCode and toString: two measures with the same
	 * values compare equal whether a field was sent as null or left out.
	 */
	@JsonIgnore
	private final EnumSet<Field> suppliedFields = EnumSet.noneOf(Field.class);

	public boolean hasField(final Field field) {
		return suppliedFields.contains(field);
	}

	@Schema(description = "Measure ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Optimistic locking version of the measure. Use its ETag in If-Match when updating or deleting", accessMode = READ_ONLY)
	private Long version;

	@Schema(description = "Responsible user (ad-username)", examples = "jo12doe", nullable = true)
	private String responsibleUser;

	@Schema(description = "Immutable MeasureType.name key, resolved within the errand municipality and namespace", examples = "INTERVENTION")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String type;

	@Schema(description = "Planned start date", examples = "2021-09-01T12:00:00Z", nullable = true)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime plannedStart;

	@Schema(description = "Planned completion date", examples = "2021-10-01T12:00:00Z", nullable = true)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime plannedComplete;

	@Schema(description = "Execution date", examples = "2021-09-15T12:00:00Z", nullable = true)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime executed;

	@Schema(description = "User who added the measure", examples = "jo12doe")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String addedByUser;

	@Schema(description = "Role of the user who added the measure", examples = "MANAGER")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	private String addedByRole;

	@Schema(description = "Goal of the measure", examples = "Improve response time", nullable = true)
	private String goal;

	@Schema(description = "Description of the measure", examples = "Detailed description of the measure", nullable = true)
	private String description;

	@Schema(description = "Accept decision. Explicit null on PATCH clears the decision", allowableValues = {
		"TRUE", "FALSE", "REWORK"
	}, nullable = true)
	@OneOf(value = {
		"TRUE", "FALSE", "REWORK"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String accept;

	@Schema(description = "Motivation for the accept decision", examples = "The measure is approved", nullable = true)
	private String acceptMotivation;

	@Schema(description = "Rework goal", examples = "Updated goal after rework", nullable = true)
	private String reworkGoal;

	@Schema(description = "Rework description", examples = "Detailed description of the rework", nullable = true)
	private String reworkDescription;

	@Schema(description = "Timestamp when the measure was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the measure was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime modified;

	public static Measure create() {
		return new Measure();
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public Measure withVersion(final Long version) {
		this.version = version;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Measure withId(final String id) {
		this.id = id;
		return this;
	}

	public String getResponsibleUser() {
		return responsibleUser;
	}

	public void setResponsibleUser(final String responsibleUser) {
		this.responsibleUser = responsibleUser;
		suppliedFields.add(Field.RESPONSIBLE_USER);
	}

	public Measure withResponsibleUser(final String responsibleUser) {
		setResponsibleUser(responsibleUser);
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
		suppliedFields.add(Field.TYPE);
	}

	public Measure withType(final String type) {
		setType(type);
		return this;
	}

	public OffsetDateTime getPlannedStart() {
		return plannedStart;
	}

	public void setPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
		suppliedFields.add(Field.PLANNED_START);
	}

	public Measure withPlannedStart(final OffsetDateTime plannedStart) {
		setPlannedStart(plannedStart);
		return this;
	}

	public OffsetDateTime getPlannedComplete() {
		return plannedComplete;
	}

	public void setPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
		suppliedFields.add(Field.PLANNED_COMPLETE);
	}

	public Measure withPlannedComplete(final OffsetDateTime plannedComplete) {
		setPlannedComplete(plannedComplete);
		return this;
	}

	public OffsetDateTime getExecuted() {
		return executed;
	}

	public void setExecuted(final OffsetDateTime executed) {
		this.executed = executed;
		suppliedFields.add(Field.EXECUTED);
	}

	public Measure withExecuted(final OffsetDateTime executed) {
		setExecuted(executed);
		return this;
	}

	public String getAddedByUser() {
		return addedByUser;
	}

	public void setAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
		suppliedFields.add(Field.ADDED_BY_USER);
	}

	public Measure withAddedByUser(final String addedByUser) {
		setAddedByUser(addedByUser);
		return this;
	}

	public String getAddedByRole() {
		return addedByRole;
	}

	public void setAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
		suppliedFields.add(Field.ADDED_BY_ROLE);
	}

	public Measure withAddedByRole(final String addedByRole) {
		setAddedByRole(addedByRole);
		return this;
	}

	public String getGoal() {
		return goal;
	}

	public void setGoal(final String goal) {
		this.goal = goal;
		suppliedFields.add(Field.GOAL);
	}

	public Measure withGoal(final String goal) {
		setGoal(goal);
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
		suppliedFields.add(Field.DESCRIPTION);
	}

	public Measure withDescription(final String description) {
		setDescription(description);
		return this;
	}

	public String getAccept() {
		return accept;
	}

	public void setAccept(final String accept) {
		this.accept = accept;
		suppliedFields.add(Field.ACCEPT);
	}

	public Measure withAccept(final String accept) {
		setAccept(accept);
		return this;
	}

	public String getAcceptMotivation() {
		return acceptMotivation;
	}

	public void setAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
		suppliedFields.add(Field.ACCEPT_MOTIVATION);
	}

	public Measure withAcceptMotivation(final String acceptMotivation) {
		setAcceptMotivation(acceptMotivation);
		return this;
	}

	public String getReworkGoal() {
		return reworkGoal;
	}

	public void setReworkGoal(final String reworkGoal) {
		this.reworkGoal = reworkGoal;
		suppliedFields.add(Field.REWORK_GOAL);
	}

	public Measure withReworkGoal(final String reworkGoal) {
		setReworkGoal(reworkGoal);
		return this;
	}

	public String getReworkDescription() {
		return reworkDescription;
	}

	public void setReworkDescription(final String reworkDescription) {
		this.reworkDescription = reworkDescription;
		suppliedFields.add(Field.REWORK_DESCRIPTION);
	}

	public Measure withReworkDescription(final String reworkDescription) {
		setReworkDescription(reworkDescription);
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Measure withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Measure withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, responsibleUser, type, plannedStart, plannedComplete, executed, addedByUser, addedByRole, goal, description, accept, acceptMotivation, reworkGoal, reworkDescription, created, modified, version);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Measure that = (Measure) o;
		return Objects.equals(id, that.id)
			&& Objects.equals(responsibleUser, that.responsibleUser)
			&& Objects.equals(type, that.type)
			&& Objects.equals(plannedStart, that.plannedStart)
			&& Objects.equals(plannedComplete, that.plannedComplete)
			&& Objects.equals(executed, that.executed)
			&& Objects.equals(addedByUser, that.addedByUser)
			&& Objects.equals(addedByRole, that.addedByRole)
			&& Objects.equals(goal, that.goal)
			&& Objects.equals(description, that.description)
			&& Objects.equals(accept, that.accept)
			&& Objects.equals(acceptMotivation, that.acceptMotivation)
			&& Objects.equals(reworkGoal, that.reworkGoal)
			&& Objects.equals(reworkDescription, that.reworkDescription)
			&& Objects.equals(created, that.created)
			&& Objects.equals(modified, that.modified)
			&& Objects.equals(version, that.version);
	}

	@Override
	public String toString() {
		return "Measure{" +
			"id='" + id + '\'' +
			", responsibleUser='" + responsibleUser + '\'' +
			", type='" + type + '\'' +
			", plannedStart=" + plannedStart +
			", plannedComplete=" + plannedComplete +
			", executed=" + executed +
			", addedByUser='" + addedByUser + '\'' +
			", addedByRole='" + addedByRole + '\'' +
			", goal='" + goal + '\'' +
			", description='" + description + '\'' +
			", accept='" + accept + '\'' +
			", acceptMotivation='" + acceptMotivation + '\'' +
			", reworkGoal='" + reworkGoal + '\'' +
			", reworkDescription='" + reworkDescription + '\'' +
			", created=" + created +
			", modified=" + modified +
			", version=" + version +
			'}';
	}
}
