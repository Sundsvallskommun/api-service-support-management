package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Base class for the metadata a namespace registers per municipality: the values an errand, or something on it, may be
 * given, each with a display name, a sort order and a deprecation flag.
 * <p>
 * There is no table, no discriminator and no polymorphic query. The subclasses share the columns declared here, not
 * storage: each gets a table of its own.
 * <p>
 * The type parameter carries the subclass through the fluent setters, so that {@code RoleEntity.create().withId(id)
 * .withNamespace(namespace)} stays a chain of {@code RoleEntity}.
 *
 * @param <T> the concrete subclass, so that the fluent setters return it.
 */
@MappedSuperclass
public abstract class AbstractMetadataEntity<T extends AbstractMetadataEntity<T>> {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "sort_order")
	private Integer sortOrder;

	@Column(name = "deprecated", nullable = false)
	private boolean deprecated;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@SuppressWarnings("unchecked")
	protected T self() {
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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public T withDisplayName(final String displayName) {
		this.displayName = displayName;
		return self();
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public T withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return self();
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	public T withDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
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

	/**
	 * Equality over the fields declared here. Subclasses with fields of their own chain to this so that inherited fields
	 * count.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		final AbstractMetadataEntity<?> that = (AbstractMetadataEntity<?>) o;
		return Objects.equals(id, that.id)
			&& Objects.equals(displayName, that.displayName)
			&& Objects.equals(sortOrder, that.sortOrder)
			&& (deprecated == that.deprecated)
			&& Objects.equals(municipalityId, that.municipalityId)
			&& Objects.equals(namespace, that.namespace)
			&& Objects.equals(created, that.created)
			&& Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, displayName, sortOrder, deprecated, municipalityId, namespace, created, modified);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + fieldsToString() + '}';
	}

	/**
	 * The fields of the entity as listed by {@link #toString()}. Subclasses with fields of their own append to this.
	 */
	protected String fieldsToString() {
		return "id='" + id + '\'' +
			", displayName='" + displayName + '\'' +
			", sortOrder=" + sortOrder +
			", deprecated=" + deprecated +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", created=" + created +
			", modified=" + modified;
	}
}
