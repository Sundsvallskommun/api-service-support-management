package se.sundsvall.supportmanagement.integration.db.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EmbeddableCustomer implements Serializable {

	private static final long serialVersionUID = -7766812807943642701L;

	@Column(name = "customer_id")
	private String id;

	@Column(name = "customer_type")
	private String type;

	public static EmbeddableCustomer create() {
		return new EmbeddableCustomer();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public EmbeddableCustomer withId(String id) {
		this.id = id;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public EmbeddableCustomer withType(String type) {
		this.type = type;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		EmbeddableCustomer other = (EmbeddableCustomer) obj;
		return Objects.equals(id, other.id) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EmbeddableCustomer [id=").append(id).append(", type=").append(type).append("]");
		return builder.toString();
	}
}
