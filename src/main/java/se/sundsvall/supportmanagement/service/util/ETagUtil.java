package se.sundsvall.supportmanagement.service.util;

import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static se.sundsvall.dept44.problem.Problem.valueOf;

public final class ETagUtil {

	private ETagUtil() {}

	public static String format(final long version) {
		return "\"" + version + "\"";
	}

	/**
	 * Validates an If-Match header against the current resource version.
	 * No-ops when ifMatch is null (opt-in enforcement).
	 * Throws 412 Precondition Failed on mismatch or weak ETags.
	 */
	public static void validateIfMatch(final String ifMatch, final Long currentVersion) {
		if (ifMatch == null) {
			return;
		}
		final var stripped = ifMatch.strip();
		if ("*".equals(stripped)) {
			return;
		}
		if (stripped.startsWith("W/")) {
			throw valueOf(PRECONDITION_FAILED, "Weak ETags are not supported in If-Match");
		}
		final var version = currentVersion != null ? currentVersion : 0L;
		for (final var tag : stripped.split(",")) {
			if (format(version).equals(tag.strip())) {
				return;
			}
		}
		throw valueOf(PRECONDITION_FAILED, "If-Match version does not match current resource version");
	}
}
