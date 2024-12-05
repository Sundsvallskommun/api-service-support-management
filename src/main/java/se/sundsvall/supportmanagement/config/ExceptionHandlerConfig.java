package se.sundsvall.supportmanagement.config;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.zalando.problem.Status.BAD_REQUEST;

import com.turkraft.springfilter.parser.InvalidSyntaxException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zalando.problem.Problem;

@Configuration
public class ExceptionHandlerConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerConfig.class);
	private static final String LOG_MESSAGE = "Mapping exception into Problem";
	private static final String TITLE = "Invalid Filter Content";

	@ControllerAdvice
	public static class ControllerExceptionHandler {

		@ExceptionHandler
		@ResponseBody
		ResponseEntity<Problem> handleInvalidSyntaxException(final InvalidSyntaxException exception) {
			LOGGER.error(LOG_MESSAGE, exception);

			return badRequest()
				.contentType(APPLICATION_PROBLEM_JSON)
				.body(Problem.builder()
					.withStatus(BAD_REQUEST)
					.withTitle(TITLE)
					.withDetail(extractMessage(exception))
					.build());
		}

		private String extractMessage(final Exception e) {
			return Optional.ofNullable(e.getMessage()).orElse(String.valueOf(e));
		}
	}
}
