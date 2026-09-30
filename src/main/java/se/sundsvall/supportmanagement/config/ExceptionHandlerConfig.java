package se.sundsvall.supportmanagement.config;

import com.turkraft.springfilter.parser.InvalidSyntaxException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

@Configuration
public class ExceptionHandlerConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerConfig.class);
	private static final String LOG_MESSAGE = "Mapping exception into Problem";
	private static final String TITLE = "Invalid Filter Content";
	// A rough estimate of how long a busy job-executor pool (label-move, purge) typically takes to free a thread up
	// again, not a promise - a caller that retries sooner or later than this loses nothing.
	private static final String RETRY_AFTER_SECONDS = "30";

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

		@ExceptionHandler
		@ResponseBody
		ResponseEntity<Problem> handleOptimisticLockingFailure(final ObjectOptimisticLockingFailureException exception) {
			LOGGER.warn("Optimistic locking failure: {}", exception.getMessage());

			return status(PRECONDITION_FAILED)
				.contentType(APPLICATION_PROBLEM_JSON)
				.body(Problem.builder()
					.withStatus(PRECONDITION_FAILED)
					.withTitle("Precondition Failed")
					.withDetail("The resource was modified by a concurrent request, please reload and retry")
					.build());
		}

		/**
		 * A job (label-move, purge) that arrives with every thread in its own executor pool already busy is refused
		 * outright rather than queued - see {@code LabelMoveConfig}'s own doc for why. Mapped here to 503 with a
		 * Retry-After header, rather than the generic 500 a {@link se.sundsvall.dept44.problem.ThrowableProblem} would
		 * otherwise carry: the pool being momentarily full is not a server error, and a caller told to retry shortly
		 * is answered more usefully than one just told something went wrong.
		 */
		@ExceptionHandler
		@ResponseBody
		ResponseEntity<Problem> handleTaskRejectedException(final TaskRejectedException exception) {
			LOGGER.warn("Task rejected: {}", exception.getMessage());

			return status(SERVICE_UNAVAILABLE)
				.header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
				.contentType(APPLICATION_PROBLEM_JSON)
				.body(Problem.builder()
					.withStatus(SERVICE_UNAVAILABLE)
					.withTitle("Service Unavailable")
					.withDetail("Too many jobs of this kind are running right now - retry shortly")
					.build());
		}

		private String extractMessage(final Exception e) {
			return Optional.ofNullable(e.getMessage()).orElse(String.valueOf(e));
		}
	}
}
