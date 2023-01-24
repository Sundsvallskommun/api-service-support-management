package se.sundsvall.supportmanagement.config;

import com.turkraft.springfilter.exception.BadFilterSyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zalando.problem.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(classes = ExceptionHandlerConfig.class)
class ExceptionHandlerConfigTest {

	@Autowired
	private ExceptionHandlerConfig.ControllerExceptionHandler controllerExceptionHandler;

	@Test
	void badFilterSyntaxExceptionIsParsedCorrectly() {
		var response = controllerExceptionHandler.handleBadFilterSyntaxException(new BadFilterSyntaxException("test exception"));

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getTitle()).isEqualTo("Invalid Filter Content");
		assertThat(response.getBody().getDetail()).isEqualTo("test exception");
		assertThat(response.getBody().getStatus()).isEqualTo(Status.BAD_REQUEST);
	}
}
