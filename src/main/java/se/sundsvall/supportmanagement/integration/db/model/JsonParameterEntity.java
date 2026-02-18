package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;

@Entity
@Table(name = "json_parameter",
	indexes = {
		@Index(name = "idx_json_parameter_errand_id", columnList = "errand_id"),
		@Index(name = "idx_json_parameter_key", columnList = "parameter_key")
	})
public class JsonParameterEntity {

	@Id
	@UuidGenerator
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_json_parameter_errand_id"))
	private ErrandEntity errandEntity;

	@Column(name = "parameter_key")
	private String key;

	@Column(name = "schema_id")
	private String schemaId;

	@JdbcTypeCode(LONG32VARCHAR)
	@Column(name = "value")
	private String value;

	public static JsonParameterEntity create() {
		return new JsonParameterEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public JsonParameterEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public JsonParameterEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public JsonParameterEntity withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getSchemaId() {
		return schemaId;
	}

	public void setSchemaId(final String schemaId) {
		this.schemaId = schemaId;
	}

	public JsonParameterEntity withSchemaId(final String schemaId) {
		this.schemaId = schemaId;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public JsonParameterEntity withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, key, schemaId, value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final JsonParameterEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(key, other.key) && Objects.equals(schemaId, other.schemaId) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "JsonParameterEntity{" +
			"id='" + id + '\'' +
			", key='" + key + '\'' +
			", schemaId='" + schemaId + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
