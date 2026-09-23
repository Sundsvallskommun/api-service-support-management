package se.sundsvall.supportmanagement.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Reads the field names out of a Lucene query string, without parsing it: what stands before a colon outside a phrase
 * is a field, and what stands on its own is a word looked for in every open field. Knows nothing about access, so
 * that what is refused is decided elsewhere.
 */
final class QueryStringFields {

	// What is inside quotes is a phrase, never a field. Written as a run of ordinary characters followed by escaped
	// ones rather than a choice repeated per character, which the engine walks recursively: a query carrying a few
	// thousand characters after an unclosed quote overflowed the stack that way, and the query comes from the client
	private static final Pattern PHRASE = Pattern.compile("\"[^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+\"");
	// A field name is whatever comes right before a colon. Nothing of a name is ever given back to find the colon,
	// since a colon is not part of a name, so the repetitions here and below are possessive
	private static final Pattern FIELD = Pattern.compile("(?<![\\w.\\\\*?-])([\\w.\\\\*?-]++):");
	// A field with its value: a group in parentheses, a range in brackets or braces, or a single term. Held to the
	// start of a name like the pattern above, which is what keeps a long word without a colon from being tried from
	// every position in it
	private static final Pattern FIELDED_TERM = Pattern.compile("(?<![\\w.\\\\*?-])[\\w.\\\\*?-]++:(?:\\([^)]*+\\)|\\[[^\\]]*+\\]|\\{[^}]*+\\}|\\S++)");
	private static final Pattern OPERATORS = Pattern.compile("\\b(?:AND|OR|NOT|TO)\\b|&&|\\|\\||[+\\-!()]");
	// The value of _exists_ is a field name too
	private static final String EXISTS = "_exists_";

	private QueryStringFields() {}

	/**
	 * The fields the query names, escapes removed, in the order they appear. A wildcard in a name is kept as it is.
	 */
	static List<String> fieldNames(final String query) {
		final var names = new ArrayList<String>();
		if (isBlank(query)) {
			return names;
		}

		final var unquoted = PHRASE.matcher(query).replaceAll(" ");
		final var matcher = FIELD.matcher(unquoted);
		while (matcher.find()) {
			names.add(unescape(matcher.group(1)));
			if (EXISTS.equals(matcher.group(1))) {
				names.add(unescape(unquoted.substring(matcher.end()).split("[\\s()]", 2)[0]));
			}
		}
		return names;
	}

	/**
	 * Whether the query holds a word that names no field: what is left once phrases, fielded terms and operators are
	 * taken out.
	 */
	static boolean hasFreeTerms(final String query) {
		if (isBlank(query)) {
			return false;
		}
		// A phrase stands in as a single word, so that a phrase given to a field stays with the field
		final var unquoted = PHRASE.matcher(query).replaceAll("phrase");
		final var unfielded = FIELDED_TERM.matcher(unquoted).replaceAll(" ");
		return !OPERATORS.matcher(unfielded).replaceAll(" ").isBlank();
	}

	static boolean isWildcard(final String fieldName) {
		return fieldName.contains("*") || fieldName.contains("?");
	}

	private static String unescape(final String fieldName) {
		return fieldName.replace("\\", "");
	}
}
