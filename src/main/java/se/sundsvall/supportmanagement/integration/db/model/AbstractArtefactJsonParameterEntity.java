package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.type.SqlTypes.LONG32VARCHAR;

/**
 * A JSON parameter of a handling artefact: structured content of the artefact, shaped like a JSON parameter of the
 * errand but not one of them.
 * <p>
 * <b>Inherited by</b> {@link StatementJsonParameterEntity}, {@link InvestigationJsonParameterEntity},
 * {@link InvestigationSectionJsonParameterEntity}, {@link DecisionJsonParameterEntity} and
 * {@link MeasureJsonParameterEntity}. Each is kept in a table beside its owner rather than in {@code json_parameter},
 * which is what makes the parameter the owner's alone: the errand neither shows it nor governs it, the key rules the
 * namespace keeps for the JSON parameters of the errand do not reach it, and a key one owner uses is not taken from the
 * errand or from any other owner. Removing the owner removes its parameters.
 * <p>
 * There is no table here and no polymorphic query - the subclasses share shape, not storage. The owner is declared in
 * each subclass, since each points at a table of its own.
 * <p>
 * The key is unique per owner, compared without regard to case as the database compares it.
 *
 * @param <T> the concrete subclass, so that the fluent setters return it.
 */
@MappedSuperclass
public abstract class AbstractArtefactJsonParameterEntity<T extends AbstractArtefactJsonParameterEntity<T>> {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "parameter_key", nullable = false)
	private String key;

	@Column(name = "schema_id")
	private String schemaId;

	@JdbcTypeCode(LONG32VARCHAR)
	@Column(name = "value", columnDefinition = "longtext")
	private String value;

	/** Carries the ETag of the parameter. */
	@Version
	@Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
	private Long version;

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

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public T withKey(final String key) {
		this.key = key;
		return self();
	}

	public String getSchemaId() {
		return schemaId;
	}

	public void setSchemaId(final String schemaId) {
		this.schemaId = schemaId;
	}

	public T withSchemaId(final String schemaId) {
		this.schemaId = schemaId;
		return self();
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public T withValue(final String value) {
		this.value = value;
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
	 * Equality over the fields declared here. The owner is left out of it since it holds this parameter in turn.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		final AbstractArtefactJsonParameterEntity<?> that = (AbstractArtefactJsonParameterEntity<?>) o;
		return Objects.equals(id, that.id)
			&& Objects.equals(key, that.key)
			&& Objects.equals(schemaId, that.schemaId)
			&& Objects.equals(value, that.value)
			&& Objects.equals(version, that.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, key, schemaId, value, version);
	}

	@Override
	public String toString() {
		return "id='" + id + '\'' +
			", key='" + key + '\'' +
			", schemaId='" + schemaId + '\'' +
			", value='" + value + '\'' +
			", version=" + version;
	}
}
