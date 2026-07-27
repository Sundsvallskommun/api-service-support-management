package se.sundsvall.supportmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.supportmanagement.Application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ApiSpecificationTest {

	@LocalServerPort
	private int port;

	@Test
	void generateOpenApiSpec() throws IOException, InterruptedException {
		final var client = HttpClient.newHttpClient();
		final var request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/api-docs"))
			.header("Accept", "application/json")
			.GET()
			.build();

		final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isNotNull().isNotBlank();

		final var json = new ObjectMapper().readValue(response.body(), Object.class);
		final var yaml = new ObjectMapper(
			YAMLFactory.builder()
				.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
				.build())
			.writerWithDefaultPrettyPrinter()
			.writeValueAsString(json);

		Files.writeString(
			Path.of("src/test/resources/api/openapi.yaml"),
			yaml,
			StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING);
	}
}
