package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A field that can not be copied to the target namespace")
public class NotCopyable {

	@Schema(description = "Name of the field that can not be copied", examples = "phases")
	private String field;

	@Schema(description = "Reason the field can not be copied", examples = "Phase history is source-specific")
	private String reason;

	public static NotCopyable create() {
		return new NotCopyable();
	}

	public String getField() {
		return field;
	}

	public void setField(final String field) {
		this.field = field;
	}

	public NotCopyable withField(final String field) {
		this.field = field;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(final String reason) {
		this.reason = reason;
	}

	public NotCopyable withReason(final String reason) {
		this.reason = reason;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, reason);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final NotCopyable other)) {
			return false;
		}
		return Objects.equals(field, other.field) && Objects.equals(reason, other.reason);
	}

	@Override
	public String toString() {
		return "NotCopyable [field=" + field + ", reason=" + reason + "]";
	}
}
