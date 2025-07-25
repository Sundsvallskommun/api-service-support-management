package se.sundsvall.supportmanagement.service.util;

import static java.util.UUID.fromString;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.dept44.support.Identifier;

public class ServiceUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUtil.class);
	private static final String MIME_ERROR_MSG = "Exception when detecting mime type of file with filename '%s'";
	private static final Tika DETECTOR = new Tika();

	private ServiceUtil() {}

	public static boolean isValidUuid(String uuid) {
		try {
			fromString(uuid);
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

	public static String detectMimeType(String fileName, byte[] byteArray) {
		try (InputStream stream = new ByteArrayInputStream(byteArray)) {
			return detectMimeTypeFromStream(fileName, stream);
		} catch (final Exception e) {
			LOGGER.warn(MIME_ERROR_MSG.formatted(sanitizeForLogging(fileName)), e);
			return APPLICATION_OCTET_STREAM_VALUE; // Return mime type for arbitrary binary files
		}
	}

	public static String detectMimeTypeFromStream(String fileName, InputStream stream) {
		try {
			return DETECTOR.detect(stream, fileName);
		} catch (final Exception e) {
			LOGGER.warn(MIME_ERROR_MSG.formatted(sanitizeForLogging(fileName)), e);
			return APPLICATION_OCTET_STREAM_VALUE; // Return mime type for arbitrary binary files
		}
	}

	public static String getAdUser() {
		return Optional.ofNullable(Identifier.get())
			.filter(identifier -> AD_ACCOUNT.equals(identifier.getType()))
			.map(Identifier::getValue)
			.orElse(null);
	}

	// TODO: Replace with LogUtils.sanitizeForLogging() (available from Dept44 v.6.0.12)
	private static String sanitizeForLogging(String input) {
		if (input == null) {
			return null;
		}
		// Replace newlines and carriage returns with spaces
		var sanitized = input.replaceAll("[\\r\\n]", " ");

		// Remove non-printable ASCII characters
		sanitized = sanitized.replaceAll("[^\\x20-\\x7E]", "");

		// Escape or remove other potentially harmful characters
		sanitized = sanitized.replaceAll("[%\\\\]", "");

		// Limit the length of the sanitized string to 100 characters
		if (sanitized.length() > 100) {
			sanitized = sanitized.substring(0, 100) + "...";
		}

		return sanitized;
	}
}
