package se.sundsvall.supportmanagement.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Guards the filter of the errand listing against the associations the errand holds for the search index alone, see
 * {@link ErrandEntity#INDEX_ONLY_ASSOCIATIONS}: spring-filter would let a filter walk them just as readily as the
 * stakeholders, and they are guarded on resources of their own.
 */
final class ErrandFilters {

	static final String FILTER_PARAMETER = "filter";
	static final String NOT_FILTERABLE = "Filtering on '%s' is not supported";

	// The association wherever it is named, not only where a field of it follows: spring-filter asks whether a
	// collection is empty and how large it is without ever naming a field of it, and "communications is not empty"
	// answers from the very rows a filter may not walk into. What may follow is anything but more of a name, which is
	// what keeps a property merely starting with the same word, such as decisionsCount, filterable
	private static final Pattern INDEX_ONLY_ASSOCIATION = Pattern.compile("(?<![\\w.])(" + String.join("|", ErrandEntity.INDEX_ONLY_ASSOCIATIONS) + ")(?!\\w)");
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
