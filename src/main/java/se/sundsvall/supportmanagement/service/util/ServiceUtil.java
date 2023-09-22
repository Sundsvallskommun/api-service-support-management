package se.sundsvall.supportmanagement.service.util;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

public class ServiceUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUtil.class);
	private static final String MIME_ERROR_MSG = "Exception when detecting mime type of file with filename '%s'";
	private static final Tika DETECTOR = new Tika();

	private ServiceUtil() {}

	public static boolean isValidUuid(String uuid) {
		try {
			UUID.fromString(uuid);
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

	public static String detectMimeType(String fileName, byte[] byteArray) {
		try (InputStream stream = new ByteArrayInputStream(byteArray)) {
			return detectMimeTypeFromStream(fileName, stream);
		} catch (final Exception e) {
			LOGGER.warn(String.format(MIME_ERROR_MSG, fileName), e);
			return APPLICATION_OCTET_STREAM_VALUE; // Return mime type for arbitrary binary files
		}
	}

	public static String detectMimeTypeFromStream(String fileName, InputStream stream) {
		try {
			return DETECTOR.detect(stream, fileName);
		} catch (final Exception e) {
			LOGGER.warn(String.format(MIME_ERROR_MSG, fileName), e);
			return APPLICATION_OCTET_STREAM_VALUE; // Return mime type for arbitrary binary files
		}
	}
}
