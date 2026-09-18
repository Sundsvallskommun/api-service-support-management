package se.sundsvall.supportmanagement.service.search;

import java.io.IOException;
import java.util.regex.Pattern;
import org.hibernate.search.util.common.SearchException;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Turns what Hibernate Search throws into an answer for the client.
 * <p>
 * Hibernate Search throws one kind of exception for everything, so the two failures a client can do something about are
 * told apart by what is inside it: a query OpenSearch could not parse is the client's to fix, an OpenSearch that could
 * not be reached is worth trying again later. Everything else is an error of ours.
 */
final class SearchProblems {

	static final String INVALID_QUERY = "The search query could not be parsed: %s";
	static final String SEARCH_UNAVAILABLE = "Search is not available at the moment";

	// The reason OpenSearch gives inside the body of its response to a query it could not parse
	private static final Pattern PARSE_FAILURE_REASON = Pattern.compile(
		"\"type\"\\s*:\\s*\"(?:query_shard_exception|parse_exception|parsing_exception)\"\\s*,\\s*\"reason\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

	private SearchProblems() {}

	/**
	 * @return the problem to answer the client with, or the exception itself when it is not the client's
	 */
	static RuntimeException toProblem(final SearchException exception) {
		if (isUnreachable(exception)) {
			return Problem.valueOf(SERVICE_UNAVAILABLE, SEARCH_UNAVAILABLE);
		}

		final var reason = PARSE_FAILURE_REASON.matcher(String.valueOf(exception.getMessage()));
		if (reason.find()) {
			return Problem.valueOf(BAD_REQUEST, INVALID_QUERY.formatted(reason.group(1).replace("\\\"", "\"")));
		}

		return exception;
	}

	private static boolean isUnreachable(final Throwable throwable) {
		for (var cause = throwable; cause != null; cause = cause.getCause()) {
			if (cause instanceof IOException) {
				return true;
			}
		}
		return false;
	}
}
