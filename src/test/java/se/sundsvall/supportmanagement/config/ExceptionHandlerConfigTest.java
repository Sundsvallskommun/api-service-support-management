package se.sundsvall.supportmanagement.config;

import com.turkraft.springfilter.parser.InvalidSyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

@SpringBootTest(classes = ExceptionHandlerConfig.class)
class ExceptionHandlerConfigTest {

	@Autowired
	private ExceptionHandlerConfig.ControllerExceptionHandler controllerExceptionHandler;

	@Test
	void badFilterSyntaxExceptionIsParsedCorrectly() {
		// The single argument constructor is deprecated
		final var exception = new InvalidSyntaxException("test exception", 1, 7, "(", null);

		final var response = controllerExceptionHandler.handleInvalidSyntaxException(exception);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getTitle()).isEqualTo("Invalid Filter Content");
		assertThat(response.getBody().getDetail()).isEqualTo("test exception");
		assertThat(response.getBody().getStatus()).isEqualTo(BAD_REQUEST);
	}

	@Test
	void optimisticLockingFailureIsMappedToPreconditionFailed() {
		final var response = controllerExceptionHandler.handleOptimisticLockingFailure(new ObjectOptimisticLockingFailureException("ErrandEntity", "some-id"));

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(PRECONDITION_FAILED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getTitle()).isEqualTo("Precondition Failed");
		assertThat(response.getBody().getDetail()).isEqualTo("The resource was modified by a concurrent request, please reload and retry");
		assertThat(response.getBody().getStatus()).isEqualTo(PRECONDITION_FAILED);
	}
}
