package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "Stakeholder model")
public class Stakeholder {

	@Schema(description = "Unique identifyer for the stakeholder", example = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	private String stakeholderId;

	@Schema(implementation = StakeholderType.class)
	@NotNull
	private StakeholderType type;

	public static Stakeholder create() {
		return new Stakeholder();
	}

	public String getStakeholderId() {
		return stakeholderId;
	}

	public void setStakeholderId(String stakeholderId) {
		this.stakeholderId = stakeholderId;
	}

	public Stakeholder withStakeholderId(String stakeholderId) {
		this.stakeholderId = stakeholderId;
		return this;
	}

	public StakeholderType getType() {
		return type;
	}

	public void setType(StakeholderType type) {
		this.type = type;
	}

	public Stakeholder withType(StakeholderType type) {
		this.type = type;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(stakeholderId, type);
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
		var other = (Stakeholder) obj;
		return Objects.equals(stakeholderId, other.stakeholderId) && type == other.type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Stakeholder{");
		sb.append("stakeholderId='").append(stakeholderId).append('\'');
		sb.append(", type=").append(type);
		sb.append('}');
		return sb.toString();
	}
}
