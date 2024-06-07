package se.sundsvall.supportmanagement.integration.db.model;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "parameter")
public class ParameterEntity {

	@Id
	@UuidGenerator
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_parameter_errand_id"))
	private ErrandEntity errandEntity;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "parameter_values",
		joinColumns = @JoinColumn(name = "parameter_id",
			foreignKey = @ForeignKey(name = "fk_parameter_values_parameter_id")
		))
	@Column(name = "value")
	private List<String> values;

	public static ParameterEntity create() {
		return new ParameterEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ParameterEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public ParameterEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public ParameterEntity withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final ParameterEntity that = (ParameterEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(errandEntity, that.errandEntity) && Objects.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandEntity, values);
	}

	@Override
	public String toString() {
		return "ParameterEntity{" +
			"id='" + id + '\'' +
			", errandEntity=" + errandEntity +
			", values=" + values +
			'}';
	}

}
