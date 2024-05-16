package se.sundsvall.supportmanagement.integration.db.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "parameter")
public class ParameterEntity {

	@Id
	@UuidGenerator
	private String id;

	@Column(name = "name")
	private String name;

	@Column(name = "value")
	private String value;

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

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ParameterEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public ParameterEntity withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public String toString() {
		return "ParameterEntity{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", value='" + value + '\'' +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final ParameterEntity that = (ParameterEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, value);
	}
}
