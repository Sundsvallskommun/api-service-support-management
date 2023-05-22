package se.sundsvall.supportmanagement.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zalando.problem.Status;

import com.turkraft.springfilter.parser.InvalidSyntaxException;

@SpringBootTest(classes = ExceptionHandlerConfig.class)
class ExceptionHandlerConfigTest {

	@Autowired
	private ExceptionHandlerConfig.ControllerExceptionHandler controllerExceptionHandler;

	@Test
	void badFilterSyntaxExceptionIsParsedCorrectly() {
		final var response = controllerExceptionHandler.handleInvalidSyntaxException(new InvalidSyntaxException("test exception"));

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getTitle()).isEqualTo("Invalid Filter Content");
		assertThat(response.getBody().getDetail()).isEqualTo("test exception");
		assertThat(response.getBody().getStatus()).isEqualTo(Status.BAD_REQUEST);
	}
}
