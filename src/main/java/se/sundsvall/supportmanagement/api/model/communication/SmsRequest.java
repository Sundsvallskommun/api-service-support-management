package se.sundsvall.supportmanagement.api.model.communication;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidMSISDN;

@Schema(description = "SmsRequest model")
public class SmsRequest {

	@NotNull
	@Size(min = 1, max = 11)
	@Schema(description = "The sender of the SMS", minLength = 1, maxLength = 11, example = "sender", requiredMode = REQUIRED)
	private String sender;

	@ValidMSISDN
	@Schema(description = "Mobile number to recipient in format +467[02369]\\d{7}", example = "+46761234567", requiredMode = REQUIRED)
	private String recipient;

	@NotBlank
	@Schema(description = "Message", requiredMode = REQUIRED)
	private String message;

	public static SmsRequest create() {
		return new SmsRequest();
	}

	public String getSender() {
		return sender;
	}

	public void setSender(final String sender) {
		this.sender = sender;
	}

	public SmsRequest withSender(final String sender) {
		this.sender = sender;
		return this;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public SmsRequest withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public SmsRequest withMessage(final String message) {
		this.message = message;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sender, recipient, message);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SmsRequest other = (SmsRequest) obj;
		return Objects.equals(sender, other.sender) &&
			Objects.equals(recipient, other.recipient) &&
			Objects.equals(message, other.message);
	}

	@Override
	public String toString() {
		return "SmsRequest [sender=" + sender + "recipient=" + recipient + ", message=" + message + "]";
	}
}
