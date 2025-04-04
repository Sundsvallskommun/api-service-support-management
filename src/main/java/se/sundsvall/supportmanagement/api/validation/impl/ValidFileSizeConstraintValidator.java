package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Objects.nonNull;
import static org.apache.commons.codec.binary.Base64.isBase64;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import se.sundsvall.supportmanagement.api.validation.ValidFileSize;

public class ValidFileSizeConstraintValidator implements ConstraintValidator<ValidFileSize, String> {
	private static final String CUSTOM_ERROR_MESSAGE_TEMPLATE = "attachment exceeds the maximum allowed size of %s bytes";

	// Defaults to 10 MB if property is not set. Observe that setting for max_allowed_packet in DB must be set to larger
	// value than byte size in validator, as DB transaction will fail if file is bigger than value allowed by DB. See
	// https://mariadb.com/kb/en/server-system-variables/#max_allowed_packet for further documentation on setting.
	@Value("${attachment.maximum.bytesize:52430000}")
	private int maximumByteSize;

	@Override
	public boolean isValid(final String base64Data, final ConstraintValidatorContext context) {
		if (nonNull(base64Data) && isBase64(base64Data)) {
			return isValidSize(base64Data, context);
		}

		return true; // Returns true if string is empty or contains non base64 encoded data
	}

	private boolean isValidSize(final String base64Data, final ConstraintValidatorContext context) {

		final var padding = determinePadding(base64Data);

		final int decodedLength = (base64Data.length() * 3) / 4 - padding;
		final boolean isValidSize = decodedLength <= maximumByteSize;

		if (!isValidSize) {
			useCustomMessageForValidation(context, String.format(CUSTOM_ERROR_MESSAGE_TEMPLATE, maximumByteSize));
		}

		return isValidSize;
	}

	private int determinePadding(final String base64Data) {
		if (base64Data.endsWith("==")) {
			return 2;
		}

		if (base64Data.endsWith("=")) {
			return 1;
		}
		return 0;
	}

	private void useCustomMessageForValidation(final ConstraintValidatorContext constraintContext, final String message) {
		constraintContext.disableDefaultConstraintViolation();
		constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}
}
