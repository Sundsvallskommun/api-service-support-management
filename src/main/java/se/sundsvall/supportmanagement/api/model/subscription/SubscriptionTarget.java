package se.sundsvall.supportmanagement.api.model.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "What a subscription targets. The id field is required when type=ERRAND and ignored when type=NAMESPACE.")
public class SubscriptionTarget {

	@NotNull
	@Schema(description = "Target type", examples = "ERRAND")
	private SubscriptionTargetType type;

	@ValidUuid(nullable = true)
	@Schema(description = "Identifier of the target. Required (errand UUID) when type=ERRAND. Must be null when type=NAMESPACE.", examples = "b82bd8ac-1507-4d9a-958d-369261eecc15")
	private String id;

	public static SubscriptionTarget create() {
		return new SubscriptionTarget();
	}

	public SubscriptionTargetType getType() {
		return type;
	}

	public void setType(final SubscriptionTargetType type) {
		this.type = type;
	}

	public SubscriptionTarget withType(final SubscriptionTargetType type) {
		this.type = type;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public SubscriptionTarget withId(final String id) {
		this.id = id;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, id);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SubscriptionTarget other = (SubscriptionTarget) obj;
		return type == other.type && Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "SubscriptionTarget{type=" + type + ", id='" + id + "'}";
	}
}
