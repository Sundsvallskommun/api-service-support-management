package se.sundsvall.supportmanagement.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Guards the filter of the errand listing against the associations the errand holds for the search index alone.
 * <p>
 * Decisions, statements, investigations and communications are collections on the errand so that the search index
 * follows them, and spring-filter would let a filter walk them just as readily as the stakeholders. They are read
 * through resources of their own, guarded on their own, and were never filterable here, so a filter that reaches for
 * them is refused rather than let past the access control of those resources.
 */
final class ErrandFilters {

	static final String FILTER_PARAMETER = "filter";
	static final String NOT_FILTERABLE = "Filtering on '%s' is not supported";

	private static final Pattern INDEX_ONLY_ASSOCIATION = Pattern.compile("(?<![\\w.])(communications|decisions|statements|investigations)\\.");
	// What is quoted is a value, not a path
	private static final Pattern QUOTED = Pattern.compile("'(?:[^'\\\\]|\\\\.)*'");

	private ErrandFilters() {}

	/**
	 * @throws org.springframework.web.ErrorResponseException 400 when the filter of the request reaches an association
	 *                                                        that is not filterable
	 */
	static void verifyFilterable(final HttpServletRequest request) {
		final var filter = request.getParameter(FILTER_PARAMETER);
		if (isNull(filter)) {
			return;
		}
		final var matcher = INDEX_ONLY_ASSOCIATION.matcher(QUOTED.matcher(filter).replaceAll(" "));
		if (matcher.find()) {
			throw Problem.valueOf(BAD_REQUEST, NOT_FILTERABLE.formatted(matcher.group(1)));
		}
	}
}
