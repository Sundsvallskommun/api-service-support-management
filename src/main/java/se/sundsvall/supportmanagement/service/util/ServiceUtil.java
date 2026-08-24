package se.sundsvall.supportmanagement.service.util;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.Access.AccessLevelEnum;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
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

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.Strings.CI;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

public class ServiceUtil {

	public static final String REQUEST_GROUP_ID_HEADER = "X-Request-Group-Id";

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUtil.class);
	private static final String MIME_ERROR_MSG = "Exception when detecting mime type of file with filename '{}'";
	private static final String HASH_ALGORITHM = "SHA-256";
	private static final Tika DETECTOR = new Tika();
	private static final ThreadLocal<String> REQUEST_GROUP_ID = new ThreadLocal<>();

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
		} catch (final Exception _) {
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

	/**
	 * Signals if sent in identifier belongs to the user making the request, which is what ownership of a subscriber, a
	 * subscription or a notification is decided on. The access mapper says nothing about ownership - being allowed to
	 * reach an errand does not make someone the owner of another user's settings for it.
	 * <p>
	 * Identifiers are stored in their wire form ("adAccount"), which is {@link Identifier#getTypeString()} rather than
	 * the {@link Identifier.Type} enum. A request without an identifier owns nothing.
	 * <p>
	 * Compared without regard to case, matching how a reporter is recognised in AccessControlService. Ad account names
	 * are not case sensitive and nothing normalises the value on the way in, so a subscriber stored as JO12DOE would
	 * otherwise be locked out of their own settings the moment they arrive as jo12doe.
	 *
	 * @param  identifierType  type of the stored identifier, in wire form
	 * @param  identifierValue value of the stored identifier
	 * @return                 true if the stored identifier is the requesting user
	 */
	public static boolean isRequestingUser(final String identifierType, final String identifierValue) {
		final var user = Identifier.get();
		return nonNull(user)
			&& StringUtils.equalsIgnoreCase(identifierType, user.getTypeString())
			&& StringUtils.equalsIgnoreCase(identifierValue, user.getValue());
	}

	public static Identifier getExecutingUser() {
		return Identifier.get();
	}

	public static void setRequestGroupId(final String requestGroupId) {
		if (StringUtils.isBlank(requestGroupId)) {
			REQUEST_GROUP_ID.remove();
		} else {
			REQUEST_GROUP_ID.set(requestGroupId);
		}
	}

	public static String getRequestGroupId() {
		return REQUEST_GROUP_ID.get();
	}

	public static void clearRequestGroupId() {
		REQUEST_GROUP_ID.remove();
	}

	public static String computeSha256Hex(final InputStream inputStream) {
		try {
			final var digest = MessageDigest.getInstance(HASH_ALGORITHM);
			final var buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				digest.update(buffer, 0, bytesRead);
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (final Exception e) {
			LOGGER.warn("Exception when computing SHA-256 hash from stream", e);
			return null;
		}
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
