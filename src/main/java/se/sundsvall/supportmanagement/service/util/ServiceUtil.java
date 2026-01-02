package se.sundsvall.supportmanagement.service.util;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.Strings.CI;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.Access.AccessLevelEnum;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;

public class ServiceUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUtil.class);
	private static final String MIME_ERROR_MSG = "Exception when detecting mime type of file with filename '{}'";
	private static final Tika DETECTOR = new Tika();

	private ServiceUtil() {}

	public static String createCacheKey(List<Access.AccessLevelEnum> filter) {
		if (CollectionUtils.isEmpty(filter)) {
			return "EMPTY";
		}

		return String.join("|", filter.stream()
			.map(AccessLevelEnum::getValue)
			.toList());
	}

	public static boolean isValidUuid(String uuid) {
		try {
			fromString(uuid);
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

	public static String detectMimeType(String filename, byte[] byteArray) {
		try (InputStream stream = new ByteArrayInputStream(byteArray)) {
			return detectMimeTypeFromStream(filename, stream);
		} catch (final Exception e) {
			return handleFault(filename, e);
		}
	}

	public static String detectMimeTypeFromStream(String filename, InputStream stream) {
		try {
			return DETECTOR.detect(stream, filename);
		} catch (final Exception e) {
			return handleFault(filename, e);
		}
	}

	public static String getAdUser() {
		return ofNullable(Identifier.get())
			.filter(identifier -> AD_ACCOUNT.equals(identifier.getType()))
			.map(Identifier::getValue)
			.orElse(null);
	}

	private static String handleFault(String filename, Exception e) {
		final var logFilename = sanitizeForLogging(filename);
		LOGGER.warn(MIME_ERROR_MSG, logFilename, e);
		return APPLICATION_OCTET_STREAM_VALUE; // Return mime type for arbitrary binary files
	}

	public static StakeholderEntity getStakeholderMatchingRole(final ErrandEntity errandEntity, String role) {
		return ofNullable(errandEntity.getStakeholders()).orElse(emptyList()).stream()
			.filter(stakeholder -> Strings.CI.equals(role, stakeholder.getRole()))
			.findFirst()
			.orElse(null);
	}

	public static Optional<String> retrieveUsername(final StakeholderEntity stakeholderEntity) {
		return ofNullable(stakeholderEntity)
			.map(StakeholderEntity::getParameters)
			.filter(Objects::nonNull)
			.map(parameters -> parameters.stream()
				.filter(parameter -> CI.equals("username", parameter.getKey()))
				.map(StakeholderParameterEntity::getValues)
				.filter(ObjectUtils::isNotEmpty)
				.flatMap(List::stream)
				.filter(StringUtils::isNotBlank)
				.findFirst())
			.orElse(Optional.empty());
	}
}
