package se.sundsvall.supportmanagement.service.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import se.sundsvall.supportmanagement.Application;

@SpringBootTest(classes = {Application.class}, webEnvironment = MOCK)
@ActiveProfiles("junit")
class BlobBuilderTest {

	@Autowired
	private BlobBuilder blobBuilder;

	@Test
	void createBlob() throws IOException, SQLException {
		// Arrange
		final var fileContent = "text-content-of-file";

		// Act
		final var blob = blobBuilder.createBlob(Base64.getEncoder().encodeToString(fileContent.getBytes(UTF_8)));

		// Assert
		assertThat(blob.getBinaryStream().readAllBytes()).isEqualTo(fileContent.getBytes());
	}

}
