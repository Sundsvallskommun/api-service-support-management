package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Why a process stopped, as the process itself describes it.
 * <p>
 * Neither field is interpreted by SM. The code tells a failed start apart from an incident raised mid process, which is
 * the distinction the five process states deliberately do not carry.
 */
@Schema(description = "Why a process failed")
public class ProcessError {

	@Schema(description = "Error code as reported by the process. Not interpreted by this service", examples = "INCIDENT")
	private String code;

	@Schema(description = "Human readable explanation of the failure. Must not carry personal data", examples = "Timeout against Employee after 30 s")
	private String message;

	public static ProcessError create() {
		return new ProcessError();
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public ProcessError withCode(final String code) {
		this.code = code;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public ProcessError withMessage(final String message) {
		this.message = message;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, message);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ProcessError other = (ProcessError) obj;
		return Objects.equals(code, other.code) && Objects.equals(message, other.message);
	}

	@Override
	public String toString() {
		return "ProcessError{" +
			"code='" + code + '\'' +
			", message='" + message + '\'' +
			'}';
	}
}
