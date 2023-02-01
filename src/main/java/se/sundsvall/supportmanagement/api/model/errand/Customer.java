package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.supportmanagement.api.validation.ValidCustomerId;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "Customer model")
@ValidCustomerId
public class Customer {

	@Schema(description = "Unique identifyer for the customer", example = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	@NotBlank
	private String id;

	@Schema(implementation = CustomerType.class)
	@NotNull
	private CustomerType type;

	public static Customer create() {
		return new Customer();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Customer withId(String id) {
		this.id = id;
		return this;
	}

	public CustomerType getType() {
		return type;
	}

	public void setType(CustomerType type) {
		this.type = type;
	}

	public Customer withType(CustomerType type) {
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
		var other = (Customer) obj;
		return Objects.equals(id, other.id) && type == other.type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Customer [id=").append(id).append(", type=").append(type).append("]");
		return builder.toString();
	}
}
