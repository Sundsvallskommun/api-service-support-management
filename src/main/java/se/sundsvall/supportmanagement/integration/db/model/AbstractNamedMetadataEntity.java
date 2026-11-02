package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Objects;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Base class for the metadata identified by a name unique within its namespace and municipality, which also stamps the
 * time it was created and last modified.
 *
 * @param <T> the concrete subclass, so that the fluent setters return it.
 */
@MappedSuperclass
public abstract class AbstractNamedMetadataEntity<T extends AbstractNamedMetadataEntity<T>> extends AbstractMetadataEntity<T> {

	@Column(name = "name", nullable = false)
	private String name;

	@PrePersist
	void onCreate() {
		setCreated(now(systemDefault()).truncatedTo(MILLIS));
	}

	@PreUpdate
	void onUpdate() {
		setModified(now(systemDefault()).truncatedTo(MILLIS));
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public T withName(final String name) {
		this.name = name;
		return self();
	}

	@Override
	public boolean equals(final Object o) {
		return super.equals(o) && Objects.equals(name, ((AbstractNamedMetadataEntity<?>) o).name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), name);
	}

	@Override
	protected String fieldsToString() {
		return super.fieldsToString() + ", name='" + name + '\'';
	}
}
