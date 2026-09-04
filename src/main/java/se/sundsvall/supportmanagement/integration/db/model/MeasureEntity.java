package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "measure",
	indexes = {
		@Index(name = "idx_measure_errand_id", columnList = "errand_id")
	})
public class MeasureEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Version
	@Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_measure_errand_id"))
	private ErrandEntity errandEntity;

	@Column(name = "responsible_user")
	private String responsibleUser;

	@Column(name = "type")
	private String type;

	@Column(name = "planned_start")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime plannedStart;

	@Column(name = "planned_complete")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime plannedComplete;

	@Column(name = "executed")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime executed;

	@Column(name = "added_by_user")
	private String addedByUser;

	@Column(name = "added_by_role")
	private String addedByRole;

	@Column(name = "goal")
	private String goal;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "accept")
	@Enumerated(EnumType.STRING)
	private Accept accept;

	@Column(name = "accept_motivation")
	private String acceptMotivation;

	@Column(name = "rework_goal")
	private String reworkGoal;

	@Column(name = "rework_description", length = 1000)
	private String reworkDescription;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@PrePersist
	void prePersist() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void preUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	public static MeasureEntity create() {
		return new MeasureEntity();
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public MeasureEntity withVersion(final Long version) {
		this.version = version;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public MeasureEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public MeasureEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public String getResponsibleUser() {
		return responsibleUser;
	}

	public void setResponsibleUser(final String responsibleUser) {
		this.responsibleUser = responsibleUser;
	}

	public MeasureEntity withResponsibleUser(final String responsibleUser) {
		this.responsibleUser = responsibleUser;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public MeasureEntity withType(final String type) {
		this.type = type;
		return this;
	}

	public OffsetDateTime getPlannedStart() {
		return plannedStart;
	}

	public void setPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
	}

	public MeasureEntity withPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
		return this;
	}

	public OffsetDateTime getPlannedComplete() {
		return plannedComplete;
	}

	public void setPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
	}

	public MeasureEntity withPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
		return this;
	}

	public OffsetDateTime getExecuted() {
		return executed;
	}

	public void setExecuted(final OffsetDateTime executed) {
		this.executed = executed;
	}

	public MeasureEntity withExecuted(final OffsetDateTime executed) {
		this.executed = executed;
		return this;
	}

	public String getAddedByUser() {
		return addedByUser;
	}

	public void setAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
	}

	public MeasureEntity withAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
		return this;
	}

	public String getAddedByRole() {
		return addedByRole;
	}

	public void setAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
	}

	public MeasureEntity withAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
		return this;
	}

	public String getGoal() {
		return goal;
	}

	public void setGoal(final String goal) {
		this.goal = goal;
	}

	public MeasureEntity withGoal(final String goal) {
		this.goal = goal;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public MeasureEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public Accept getAccept() {
		return accept;
	}

	public void setAccept(final Accept accept) {
		this.accept = accept;
	}

	public MeasureEntity withAccept(final Accept accept) {
		this.accept = accept;
		return this;
	}

	public String getAcceptMotivation() {
		return acceptMotivation;
	}

	public void setAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
	}

	public MeasureEntity withAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
		return this;
	}

	public String getReworkGoal() {
		return reworkGoal;
	}

	public void setReworkGoal(final String reworkGoal) {
		this.reworkGoal = reworkGoal;
	}

	public MeasureEntity withReworkGoal(final String reworkGoal) {
		this.reworkGoal = reworkGoal;
		return this;
	}

	public String getReworkDescription() {
		return reworkDescription;
	}

	public void setReworkDescription(final String reworkDescription) {
		this.reworkDescription = reworkDescription;
	}

	public MeasureEntity withReworkDescription(final String reworkDescription) {
		this.reworkDescription = reworkDescription;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public MeasureEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public MeasureEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final MeasureEntity that = (MeasureEntity) o;
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
			&& accept == that.accept
			&& Objects.equals(acceptMotivation, that.acceptMotivation)
			&& Objects.equals(reworkGoal, that.reworkGoal)
			&& Objects.equals(reworkDescription, that.reworkDescription)
			&& Objects.equals(created, that.created)
			&& Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, responsibleUser, type, plannedStart, plannedComplete, executed, addedByUser, addedByRole, goal, description, accept, acceptMotivation, reworkGoal, reworkDescription, created, modified);
	}

	@Override
	public String toString() {
		return "MeasureEntity{" +
			"id='" + id + '\'' +
			", errandEntity=" + (errandEntity != null ? errandEntity.getId() : "null") +
			", responsibleUser='" + responsibleUser + '\'' +
			", type='" + type + '\'' +
			", plannedStart=" + plannedStart +
			", plannedComplete=" + plannedComplete +
			", executed=" + executed +
			", addedByUser='" + addedByUser + '\'' +
			", addedByRole='" + addedByRole + '\'' +
			", goal='" + goal + '\'' +
			", description='" + description + '\'' +
			", accept=" + accept +
			", acceptMotivation='" + acceptMotivation + '\'' +
			", reworkGoal='" + reworkGoal + '\'' +
			", reworkDescription='" + reworkDescription + '\'' +
			", created=" + created +
			", modified=" + modified +
			", version=" + version +
			'}';
	}
}
