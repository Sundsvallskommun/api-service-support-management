package se.sundsvall.supportmanagement.api.model.communication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidMSISDN;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "SmsRequest model")
public class SmsRequest {

	@NotNull
	@Size(min = 1, max = 11)
	@Schema(description = "The sender of the SMS", minLength = 1, maxLength = 11, examples = "sender", requiredMode = REQUIRED)
	private String sender;

	@ValidMSISDN
	@Schema(description = "Mobile number to recipient in format +467[02369]\\d{7}", examples = "+46701740605", requiredMode = REQUIRED)
	private String recipient;

	@NotBlank
	@Schema(description = "Message", requiredMode = REQUIRED)
	private String message;

	@Schema(description = "Indicates if the message is internal", examples = "false")
	private boolean internal;

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

	public boolean getInternal() {
		return internal;
	}

	public void setInternal(final boolean internal) {
		this.internal = internal;
	}

	public SmsRequest withInternal(final boolean internal) {
		this.internal = internal;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final SmsRequest that = (SmsRequest) o;
		return Objects.equals(sender, that.sender) && Objects.equals(recipient, that.recipient) && Objects.equals(message, that.message) && Objects.equals(internal, that.internal);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sender, recipient, message, internal);
	}

	@Override
	public String toString() {
		return "SmsRequest{" +
			"sender='" + sender + '\'' +
			", recipient='" + recipient + '\'' +
			", message='" + message + '\'' +
			", internal=" + internal +
			'}';
	}
}
