package se.sundsvall.supportmanagement.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

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
	void detectMimeTypeThrowsException() {
		assertThat(ServiceUtil.detectMimeType(null, null)).isEqualTo("application/octet-stream");
	}

	@Test
	void detectMimeType() throws IOException {
		// IMAGE
		assertThat(ServiceUtil.detectMimeType(IMG_FILE_NAME, getBytes(PATH + IMG_FILE_NAME))).isEqualTo("image/jpeg");
		assertThat(ServiceUtil.detectMimeType(null, getBytes(PATH + IMG_FILE_NAME))).isEqualTo("image/jpeg");

		// DOC
		assertThat(ServiceUtil.detectMimeType(DOC_FILE_NAME, getBytes(PATH + DOC_FILE_NAME))).isEqualTo("application/msword");
		assertThat(ServiceUtil.detectMimeType(null, getBytes(PATH + DOC_FILE_NAME))).isEqualTo("application/x-tika-msoffice");

		// DOCX
		assertThat(ServiceUtil.detectMimeType(DOCX_FILE_NAME, getBytes(PATH + DOCX_FILE_NAME))).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		assertThat(ServiceUtil.detectMimeType(null, getBytes(PATH + DOCX_FILE_NAME))).isEqualTo("application/x-tika-ooxml");

		// PDF
		assertThat(ServiceUtil.detectMimeType(PDF_FILE_NAME, getBytes(PATH + PDF_FILE_NAME))).isEqualTo("application/pdf");
		assertThat(ServiceUtil.detectMimeType(null, getBytes(PATH + PDF_FILE_NAME))).isEqualTo("application/pdf");

		// TEXT
		assertThat(ServiceUtil.detectMimeType(TXT_FILE_NAME, getBytes(PATH + TXT_FILE_NAME))).isEqualTo("text/plain");
		assertThat(ServiceUtil.detectMimeType(null, getBytes(PATH + TXT_FILE_NAME))).isEqualTo("text/plain");
	}

	private byte[] getBytes(String path) throws IOException {
		return new ClassPathResource(path).getInputStream().readAllBytes();
	}
}
