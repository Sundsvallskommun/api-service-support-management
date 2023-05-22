package se.sundsvall.supportmanagement.config;

import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zalando.problem.Problem;

import com.turkraft.springfilter.parser.InvalidSyntaxException;

@Configuration
public class ExceptionHandlerConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerConfig.class);
	private static final String LOG_MESSAGE = "Mapping exception into Problem";
	private static final String TITLE = "Invalid Filter Content";

	@ControllerAdvice
	public static class ControllerExceptionHandler {

		@ExceptionHandler
		@ResponseBody
		public ResponseEntity<Problem> handleInvalidSyntaxException(InvalidSyntaxException exception) {
			LOGGER.info(LOG_MESSAGE, exception);

			final var errorResponse = Problem.builder()
				.withStatus(BAD_REQUEST)
				.withTitle(TITLE)
				.withDetail(extractMessage(exception))
				.build();

			return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(errorResponse);
		}

		private String extractMessage(Exception e) {
			return Optional.ofNullable(e.getMessage()).orElse(String.valueOf(e));
		}
	}
}
