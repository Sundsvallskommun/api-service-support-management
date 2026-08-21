package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Measure model")
public class Measure {

	@Schema(description = "Measure ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Responsible user (ad-username)", examples = "jo12doe")
	private String responsibleUser;

	@Schema(description = "Type of measure", examples = "INTERVENTION")
	@NotBlank
	private String type;

	@Schema(description = "Planned start date", examples = "2021-09-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime plannedStart;

	@Schema(description = "Planned completion date", examples = "2021-10-01T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime plannedComplete;

	@Schema(description = "Execution date", examples = "2021-09-15T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime executed;

	@Schema(description = "User who added the measure", examples = "jo12doe")
	@NotBlank
	private String addedByUser;

	@Schema(description = "Role of the user who added the measure", examples = "MANAGER")
	@NotBlank
	private String addedByRole;

	@Schema(description = "Goal of the measure", examples = "Improve response time")
	private String goal;

	@Schema(description = "Description of the measure", examples = "Detailed description of the measure")
	private String description;

	@Schema(description = "Accept status", examples = "TRUE", nullable = true)
	@OneOf(value = {
		"TRUE", "FALSE", "REWORK"
	}, nullable = true)
	private String accept;

	@Schema(description = "Motivation for the accept decision", examples = "The measure is approved")
	private String acceptMotivation;

	@Schema(description = "Rework goal", examples = "Updated goal after rework")
	private String reworkGoal;

	@Schema(description = "Rework description", examples = "Detailed description of the rework")
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
	}

	public Measure withResponsibleUser(final String responsibleUser) {
		this.responsibleUser = responsibleUser;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Measure withType(final String type) {
		this.type = type;
		return this;
	}

	public OffsetDateTime getPlannedStart() {
		return plannedStart;
	}

	public void setPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
	}

	public Measure withPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
		return this;
	}

	public OffsetDateTime getPlannedComplete() {
		return plannedComplete;
	}

	public void setPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
	}

	public Measure withPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
		return this;
	}

	public OffsetDateTime getExecuted() {
		return executed;
	}

	public void setExecuted(final OffsetDateTime executed) {
		this.executed = executed;
	}

	public Measure withExecuted(final OffsetDateTime executed) {
		this.executed = executed;
		return this;
	}

	public String getAddedByUser() {
		return addedByUser;
	}

	public void setAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
	}

	public Measure withAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
		return this;
	}

	public String getAddedByRole() {
		return addedByRole;
	}

	public void setAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
	}

	public Measure withAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
		return this;
	}

	public String getGoal() {
		return goal;
	}

	public void setGoal(final String goal) {
		this.goal = goal;
	}

	public Measure withGoal(final String goal) {
		this.goal = goal;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Measure withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getAccept() {
		return accept;
	}

	public void setAccept(final String accept) {
		this.accept = accept;
	}

	public Measure withAccept(final String accept) {
		this.accept = accept;
		return this;
	}

	public String getAcceptMotivation() {
		return acceptMotivation;
	}

	public void setAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
	}

	public Measure withAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
		return this;
	}

	public String getReworkGoal() {
		return reworkGoal;
	}

	public void setReworkGoal(final String reworkGoal) {
		this.reworkGoal = reworkGoal;
	}

	public Measure withReworkGoal(final String reworkGoal) {
		this.reworkGoal = reworkGoal;
		return this;
	}

	public String getReworkDescription() {
		return reworkDescription;
	}

	public void setReworkDescription(final String reworkDescription) {
		this.reworkDescription = reworkDescription;
	}

	public Measure withReworkDescription(final String reworkDescription) {
		this.reworkDescription = reworkDescription;
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
		return Objects.hash(id, responsibleUser, type, plannedStart, plannedComplete, executed, addedByUser, addedByRole, goal, description, accept, acceptMotivation, reworkGoal, reworkDescription, created, modified);
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
			&& Objects.equals(modified, that.modified);
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
			'}';
	}
}
