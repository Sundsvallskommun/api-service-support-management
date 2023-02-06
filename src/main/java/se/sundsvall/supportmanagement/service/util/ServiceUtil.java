package se.sundsvall.supportmanagement.service.util;

import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import org.overviewproject.mime_types.MimeTypeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUtil.class);
	private static final MimeTypeDetector DETECTOR = new MimeTypeDetector();

	private ServiceUtil() {}

	public static boolean isValidUuid(String uuid) {
		try {
			UUID.fromString(uuid);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String detectMimeType(String fileName, byte[] byteArray) {
		try (InputStream stream = new ByteArrayInputStream(byteArray)) {
			return DETECTOR.detectMimeType(fileName, stream);
		} catch (Exception e) {
			LOGGER.warn(String.format("Exception when detecting mime type of file with filename '%s'", fileName), e);
			return APPLICATION_OCTET_STREAM_VALUE; // Return mime type for arbitrary binary files
		}
	}
}
