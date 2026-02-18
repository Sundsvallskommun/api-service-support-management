package se.sundsvall.supportmanagement.service.util;

import generated.se.sundsvall.accessmapper.Access;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceUtilTest {

	private static final String PATH = "mimetype_files/";
	private static final String IMG_FILE_NAME = "image.jpg";
	private static final String DOC_FILE_NAME = "document.doc";
	private static final String DOCX_FILE_NAME = "document.docx";
	private static final String PDF_FILE_NAME = "document.pdf";
	private static final String TXT_FILE_NAME = "document.txt";

	@ParameterizedTest
	@MethodSource("toValidUuidsStreamArguments")
	void isValidUuid(String uuid, boolean expectedResult) {
		assertThat(ServiceUtil.isValidUuid(uuid)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> toValidUuidsStreamArguments() {
		return Stream.of(
			Arguments.of(UUID.randomUUID().toString(), true),
			Arguments.of("not-valid-uuid", false),
			Arguments.of("", false),
			Arguments.of(null, false));
	}

	@Test
	void detectMimeTypeWhenAllParametersAreNullThrowsException() {
		assertThat(ServiceUtil.detectMimeType(null, null)).isEqualTo("application/octet-stream");
		assertThat(ServiceUtil.detectMimeTypeFromStream(null, null)).isEqualTo("application/octet-stream");
	}

	@Test
	void detectMimeTypeWhenInputStreamIsNullThrowsException() {
		assertThat(ServiceUtil.detectMimeType("filename", null)).isEqualTo("application/octet-stream");
		assertThat(ServiceUtil.detectMimeTypeFromStream("filename", null)).isEqualTo("application/octet-stream");
	}

	@Test
	void detectMimeType() throws IOException {
		// IMAGE
		assertThat(ServiceUtil.detectMimeTypeFromStream(IMG_FILE_NAME, getStream(PATH + IMG_FILE_NAME))).isEqualTo("image/jpeg");
		assertThat(ServiceUtil.detectMimeType(IMG_FILE_NAME, getStream(PATH + IMG_FILE_NAME).readAllBytes())).isEqualTo("image/jpeg");
		assertThat(ServiceUtil.detectMimeTypeFromStream(null, getStream(PATH + IMG_FILE_NAME))).isEqualTo("image/jpeg");

		// DOC
		assertThat(ServiceUtil.detectMimeTypeFromStream(DOC_FILE_NAME, getStream(PATH + DOC_FILE_NAME))).isEqualTo("application/msword");
		assertThat(ServiceUtil.detectMimeType(DOC_FILE_NAME, getStream(PATH + DOC_FILE_NAME).readAllBytes())).isEqualTo("application/msword");
		assertThat(ServiceUtil.detectMimeTypeFromStream(null, getStream(PATH + DOC_FILE_NAME))).isEqualTo("application/x-tika-msoffice");

		// DOCX
		assertThat(ServiceUtil.detectMimeTypeFromStream(DOCX_FILE_NAME, getStream(PATH + DOCX_FILE_NAME))).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		assertThat(ServiceUtil.detectMimeType(DOCX_FILE_NAME, getStream(PATH + DOCX_FILE_NAME).readAllBytes())).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		assertThat(ServiceUtil.detectMimeTypeFromStream(null, getStream(PATH + DOCX_FILE_NAME))).isEqualTo("application/x-tika-ooxml");

		// PDF
		assertThat(ServiceUtil.detectMimeTypeFromStream(PDF_FILE_NAME, getStream(PATH + PDF_FILE_NAME))).isEqualTo("application/pdf");
		assertThat(ServiceUtil.detectMimeType(PDF_FILE_NAME, getStream(PATH + PDF_FILE_NAME).readAllBytes())).isEqualTo("application/pdf");
		assertThat(ServiceUtil.detectMimeTypeFromStream(null, getStream(PATH + PDF_FILE_NAME))).isEqualTo("application/pdf");

		// TEXT
		assertThat(ServiceUtil.detectMimeTypeFromStream(TXT_FILE_NAME, getStream(PATH + TXT_FILE_NAME))).isEqualTo("text/plain");
		assertThat(ServiceUtil.detectMimeType(TXT_FILE_NAME, getStream(PATH + TXT_FILE_NAME).readAllBytes())).isEqualTo("text/plain");
		assertThat(ServiceUtil.detectMimeTypeFromStream(null, getStream(PATH + TXT_FILE_NAME))).isEqualTo("text/plain");
	}

	private InputStream getStream(String path) throws IOException {
		return new ClassPathResource(path).getInputStream();
	}

	@ParameterizedTest
	@NullAndEmptySource
	void createCacheKeyFromNullOrEmpty(List<Access.AccessLevelEnum> filter) {
		assertThat(ServiceUtil.createCacheKey(filter)).isEqualTo("EMPTY");
	}

	@Test
	void createCacheKey() {
		assertThat(ServiceUtil.createCacheKey(List.of(RW))).isEqualTo("RW");
		assertThat(ServiceUtil.createCacheKey(List.of(RW, LR))).isEqualTo("RW|LR");
		assertThat(ServiceUtil.createCacheKey(List.of(LR, RW, R))).isEqualTo("LR|RW|R");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"reporter", "REPORTER", "rEpOrTeR"
	})
	void getStakeholderMatchingRole(String role) {
		final var reporterStakeholder = StakeholderEntity.create().withRole("REPORTER");
		final var errandEntity = ErrandEntity.create().withStakeholders(List.of(reporterStakeholder));

		final var result = ServiceUtil.getStakeholderMatchingRole(errandEntity, role);

		assertThat(result).isSameAs(reporterStakeholder);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void getStakeholderMatchingRoleWhenNoStakeholders(List<StakeholderEntity> stakeholders) {
		final var errandEntity = ErrandEntity.create().withStakeholders(stakeholders);

		assertThat(ServiceUtil.getStakeholderMatchingRole(errandEntity, "REPORTER")).isNull();

	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"non-matching-role"
	})
	void getStakeholderMatchingRoleWhenNoMatch(String role) {
		final var reporterStakeholder = StakeholderEntity.create().withRole("REPORTER");
		final var errandEntity = ErrandEntity.create().withStakeholders(List.of(reporterStakeholder));

		assertThat(ServiceUtil.getStakeholderMatchingRole(errandEntity, role)).isNull();
	}

	@Test
	void retrieveUsername() {
		final var usernameValue = "usernameValue";
		assertThat(ServiceUtil.retrieveUsername(StakeholderEntity.create().withParameters(List.of(StakeholderParameterEntity.create()
			.withKey("username")
			.withValues(List.of(" ", "", usernameValue))))))
			.isPresent().hasValue(usernameValue);

	}

	@Test
	void retrieveUsernameFromNullOrEmpty() {
		assertThat(ServiceUtil.retrieveUsername(null)).isEmpty();
		assertThat(ServiceUtil.retrieveUsername(StakeholderEntity.create())).isEmpty();
		assertThat(ServiceUtil.retrieveUsername(StakeholderEntity.create().withParameters(emptyList()))).isEmpty();
		assertThat(ServiceUtil.retrieveUsername(StakeholderEntity.create().withParameters(List.of(StakeholderParameterEntity.create().withKey("not-username"))))).isEmpty();
		assertThat(ServiceUtil.retrieveUsername(StakeholderEntity.create().withParameters(List.of(StakeholderParameterEntity.create().withKey("username"))))).isEmpty();
		assertThat(ServiceUtil.retrieveUsername(StakeholderEntity.create().withParameters(List.of(StakeholderParameterEntity.create().withKey("username").withValues(emptyList()))))).isEmpty();
		assertThat(ServiceUtil.retrieveUsername(StakeholderEntity.create().withParameters(List.of(StakeholderParameterEntity.create().withKey("username").withValues(List.of("")))))).isEmpty();
		assertThat(ServiceUtil.retrieveUsername(StakeholderEntity.create().withParameters(List.of(StakeholderParameterEntity.create().withKey("username").withValues(List.of(" ")))))).isEmpty();
	}
}
