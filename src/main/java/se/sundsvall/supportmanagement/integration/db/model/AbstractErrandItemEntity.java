package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * Base class for the handling artefacts of an errand: statement, investigation, decision and measure.
 * <p>
 * <b>Inherited ONLY by</b> {@link StatementEntity}, {@link InvestigationEntity}, {@link DecisionEntity} and
 * {@link MeasureEntity}. What they have in common is that each documents a step of handling with a life cycle of its
 * own, a deadline of its own and an outcome of its own. Stakeholder, attachment and notification belong to the errand
 * too but have none of that, and must not inherit from here - dragging {@code dueAt}, {@code status} and {@code type}
 * into them is exactly the mistake this javadoc exists to prevent.
 * <p>
 * There is no table, no discriminator and no polymorphic query. The subclasses share shape, not storage: each gets a
 * table of its own and invariants of its own. What the sharing buys is that the compiler keeps the four in step - a new
 * common column is added in one place, and none of them can drift away from the others in silence.
 * <p>
 * The type parameter carries the subclass through the fluent setters, so that {@code StatementEntity.create()
 * .withId(id).withTitle(title)} stays a chain of {@code StatementEntity} rather than degrading to this class at the
 * first inherited field.
 * <p>
 * The association to the errand is declared here but named per subclass with {@code @AssociationOverride}: InnoDB has
 * one namespace for foreign keys across the whole schema, so four subclasses sharing a constraint name would collide.
 *
 * @param <T> the concrete subclass, so that the fluent setters return it.
 */
@MappedSuperclass
public abstract class AbstractErrandItemEntity<T extends AbstractErrandItemEntity<T>> {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	// Three of the four are not a collection on the errand - they are read through their own resources - so nothing in JPA
	// cascades their removal. The database does, and this is what says so in one place for all four.
	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_errand_item_errand_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ErrandEntity errandEntity;

	/**
	 * Redundant against the errand, and deliberately so: the resources filter and authorize on it without loading the
	 * errand.
	 */
	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	/** The word of the line of business, not an enum. */
	@Column(name = "type", length = 128)
	private String type;

	@Enumerated(STRING)
	@JdbcTypeCode(VARCHAR)
	@Column(name = "status", length = 32, nullable = false)
	private ItemStatus status;

	@Column(name = "title")
	private String title;

	@Column(name = "description", length = LONG32)
	private String description;

	/** The deadline. A passed deadline is a question the business asks about all four. */
	@Column(name = "due_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime dueAt;

	@Column(name = "completed_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime completedAt;

	/** AD account on a manual write, consumer name when a process writes. */
	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "modified_by")
	private String modifiedBy;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	/** Carries the ETag of the resource. */
	@Version
	@Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
	private Long version;

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	@SuppressWarnings("unchecked")
	private T self() {
		return (T) this;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public T withId(final String id) {
		this.id = id;
		return self();
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public T withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return self();
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public T withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return self();
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public T withNamespace(final String namespace) {
		this.namespace = namespace;
		return self();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public T withType(final String type) {
		this.type = type;
		return self();
	}

	public ItemStatus getStatus() {
		return status;
	}

	public void setStatus(final ItemStatus status) {
		this.status = status;
	}

	public T withStatus(final ItemStatus status) {
		this.status = status;
		return self();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public T withTitle(final String title) {
		this.title = title;
		return self();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public T withDescription(final String description) {
		this.description = description;
		return self();
	}

	public OffsetDateTime getDueAt() {
		return dueAt;
	}

	public void setDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
	}

	public T withDueAt(final OffsetDateTime dueAt) {
		this.dueAt = dueAt;
		return self();
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public T withCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
		return self();
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public T withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return self();
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public T withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return self();
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public T withCreated(final OffsetDateTime created) {
		this.created = created;
		return self();
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public T withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return self();
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public T withVersion(final Long version) {
		this.version = version;
		return self();
	}

	/**
	 * Equality over the fields declared here. Subclasses chain to this so that inherited fields count, and the errand is
	 * left out of it since it points back at an object that holds this one.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		final AbstractErrandItemEntity<?> that = (AbstractErrandItemEntity<?>) o;
		return Objects.equals(id, that.id)
			&& Objects.equals(municipalityId, that.municipalityId)
			&& Objects.equals(namespace, that.namespace)
			&& Objects.equals(type, that.type)
			&& (status == that.status)
			&& Objects.equals(title, that.title)
			&& Objects.equals(description, that.description)
			&& Objects.equals(dueAt, that.dueAt)
			&& Objects.equals(completedAt, that.completedAt)
			&& Objects.equals(createdBy, that.createdBy)
			&& Objects.equals(modifiedBy, that.modifiedBy)
			&& Objects.equals(created, that.created)
			&& Objects.equals(modified, that.modified)
			&& Objects.equals(version, that.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, type, status, title, description, dueAt, completedAt, createdBy, modifiedBy, created, modified, version);
	}

	@Override
	public String toString() {
		return "id='" + id + '\'' +
			", errandEntity=" + (errandEntity != null ? errandEntity.getId() : "null") +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", type='" + type + '\'' +
			", status=" + status +
			", title='" + title + '\'' +
			", description='" + description + '\'' +
			", dueAt=" + dueAt +
			", completedAt=" + completedAt +
			", createdBy='" + createdBy + '\'' +
			", modifiedBy='" + modifiedBy + '\'' +
			", created=" + created +
			", modified=" + modified +
			", version=" + version;
	}
}
