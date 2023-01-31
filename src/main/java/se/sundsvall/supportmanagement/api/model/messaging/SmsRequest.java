package se.sundsvall.supportmanagement.api.model.messaging;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.dept44.common.validators.annotation.ValidMobileNumber;

public class SmsRequest {

	@NotNull
	@Size(min = 1, max = 11)
	@Schema(description = "The sender of the SMS", maxLength = 11, example = "sender", requiredMode = REQUIRED)
	private String sender;

	@ValidMobileNumber
	@Schema(description = "Mobile number to recipient in format 07[02369]\\d{7}", example = "0761234567", requiredMode = REQUIRED)
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

	public void setSender(String sender) {
		this.sender = sender;
	}

	public SmsRequest withSender(String sender) {
		this.sender = sender;
		return this;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public SmsRequest withRecipient(String recipient) {
		this.recipient = recipient;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public SmsRequest withMessage(String message) {
		this.message = message;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sender, recipient, message);
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
		SmsRequest other = (SmsRequest) obj;
		return Objects.equals(sender, other.sender) &&
			Objects.equals(recipient, other.recipient) &&
			Objects.equals(message, other.message);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SmsRequest [sender=").append(sender).append("recipient=").append(recipient).append(", message=").append(message).append("]");
		return builder.toString();
	}

}
