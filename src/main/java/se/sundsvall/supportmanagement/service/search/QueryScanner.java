package se.sundsvall.supportmanagement.service.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a Lucene query string the way the parser behind {@code query_string} reads it.
 * <p>
 * What a search may look in is decided from the fields the query names, so reading the query wrongly is not a wrong
 * answer but an open door: a name we do not see is a field the index searches all the same. Patterns were tried for
 * this and kept being wrong about one shape or another - a space before the colon, an ideographic space, a name opened
 * by a minus, a unicode escape, a quote that was escaped - so this walks the query once instead, holding the state the
 * grammar holds: escaped, quoted, inside a regular expression, inside brackets.
 * <p>
 * Two things it reports are what the access rules are built on. The fields the query names, each with the span it
 * occupies, so that a name can be rewritten without touching anything else. And the colons it could not account for:
 * a colon that is not part of a reference it parsed, and not inside a phrase, a regular expression or a range, is a
 * fielded term this does not understand, which a restricted search refuses rather than passes on.
 */
final class QueryScanner {

	/**
	 * The ideographic space, which the parser passes over and Java's own class of whitespace does not hold. Written as
	 * the escape it is: the character itself cannot be seen in a source file, and what cannot be seen is what a tool
	 * normalising whitespace quietly takes away - here that would open the gap it is here to close.
	 */
	private static final char IDEOGRAPHIC_SPACE = '\u3000';

	/** What the parser passes over between tokens. */
	private static final String WHITESPACE = " \t\n\r" + IDEOGRAPHIC_SPACE;

	/** Characters a field name is made of besides letters and digits, escapes aside. */
	private static final String NAME_PUNCTUATION = "_.-*?@";

	/** Words the grammar owns, which name no field and are no terms of their own. */
	private static final List<String> OPERATORS = List.of("AND", "OR", "NOT", "TO");

	/** The field whose value is itself a field name. */
	private static final String EXISTS = "_exists_";

	private final String query;
	private final List<FieldReference> fields = new ArrayList<>();
	private final List<Span> freeTerms = new ArrayList<>();
	private int unaccountedColons;
	private int position;

	private QueryScanner(final String query) {
		this.query = query;
	}

	/**
	 * @param query the query as the client wrote it, null or blank giving an empty scan
	 */
	static Scan scan(final String query) {
		if (query == null || query.isBlank()) {
			return new Scan(List.of(), List.of(), 0);
		}
		final var scanner = new QueryScanner(query);
		scanner.run();
		return new Scan(List.copyOf(scanner.fields), List.copyOf(scanner.freeTerms), scanner.unaccountedColons);
	}

	/**
	 * Whether a name stands for more than itself. A wildcard may name a field nobody enumerated, so only a route that is
	 * held to nothing may search one.
	 */
	static boolean isWildcard(final String name) {
		return name.indexOf('*') >= 0 || name.indexOf('?') >= 0;
	}

	/** A stretch of the query, from start inclusive to end exclusive. */
	record Span(int start, int end) {}

	/**
	 * A field the query names.
	 *
	 * @param name   the name as the parser reads it, escapes resolved
	 * @param inName where the name stands, which is what a rewrite replaces
	 * @param inTerm where the whole term stands, the leading operator excluded, which is what a rewrite into several
	 *               alternatives replaces
	 * @param exists whether the field is named as the value of {@code _exists_} rather than by a colon of its own
	 */
	record FieldReference(String name, Span inName, Span inTerm, boolean exists) {}

	/**
	 * What one query names.
	 *
	 * @param fields            the fields it names, in the order they appear
	 * @param freeTerms         the words and phrases it carries that name no field
	 * @param unaccountedColons colons this could not read as part of a reference, see {@link QueryScanner}
	 */
	record Scan(List<FieldReference> fields, List<Span> freeTerms, int unaccountedColons) {

		List<String> fieldNames() {
			return fields.stream().map(FieldReference::name).toList();
		}

		boolean hasFreeTerms() {
			return !freeTerms.isEmpty();
		}

		/** Whether the query holds something this could not read, which a restricted search may not pass on. */
		boolean isFullyRead() {
			return unaccountedColons == 0;
		}
	}

	private void run() {
		while (position < query.length()) {
			final var character = query.charAt(position);

			if (WHITESPACE.indexOf(character) >= 0 || character == '(' || character == ')') {
				position++;
			} else if (character == '+' || character == '-' || character == '!') {
				// An operator where a term begins, part of a name nowhere: a name is never read from here
				position++;
			} else if (character == '"') {
				freeTerms.add(readPhrase());
			} else if (character == '/') {
				freeTerms.add(readRegex());
			} else if (character == '[' || character == '{') {
				// A range standing on its own names nothing, and is no word either
				readRange();
			} else if (character == ':') {
				// A colon where no name stands before it, so no reference was read from it
				unaccountedColons++;
				position++;
			} else {
				readTermOrReference();
			}
		}
	}

	/**
	 * A word, and the field reference it makes where a colon follows it. The parser passes over whitespace between the
	 * name and its colon, so this does too.
	 */
	private void readTermOrReference() {
		final var start = position;
		final var name = readName();
		final var nameEnd = position;

		if (nameEnd == start) {
			// Punctuation the grammar owns rather than a word: a boost, a fuzziness, an ampersand. Nothing names a field
			// here, and stepping over it is what keeps the walk moving
			position++;
			return;
		}

		final var afterWhitespace = skipWhitespace(position);

		if (afterWhitespace >= query.length() || query.charAt(afterWhitespace) != ':') {
			if (!OPERATORS.contains(name)) {
				freeTerms.add(new Span(start, nameEnd));
			}
			return;
		}

		position = afterWhitespace + 1;
		final var value = readValue();

		if (EXISTS.equals(name)) {
			// The value names the field, whether it stands bare, in a group or in quotes
			final var named = unwrap(value);
			fields.add(new FieldReference(decode(query.substring(named.start(), named.end())), named, new Span(start, value.end()), true));
			return;
		}

		fields.add(new FieldReference(name, new Span(start, nameEnd), new Span(start, value.end()), false));
	}

	/**
	 * The value of a field, which spans a group, a range, a phrase or a regular expression whole rather than stopping
	 * inside one.
	 */
	private Span readValue() {
		position = skipWhitespace(position);
		if (position >= query.length()) {
			return new Span(position, position);
		}

		return switch (query.charAt(position)) {
			case '(' -> readBalanced('(', ')');
			case '[' -> readBalanced('[', ']');
			case '{' -> readBalanced('{', '}');
			case '"' -> readPhrase();
			case '/' -> readRegex();
			default -> readBareValue();
		};
	}

	/** A name, escapes resolved: what the parser has once it is done reading the characters. */
	private String readName() {
		final var start = position;
		while (position < query.length()) {
			final var character = query.charAt(position);
			if (character == '\\') {
				position += escapeLength(position);
			} else if (isNameCharacter(character)) {
				position++;
			} else {
				break;
			}
		}
		return decode(query.substring(start, position));
	}

	private Span readBareValue() {
		final var start = position;
		while (position < query.length()) {
			final var character = query.charAt(position);
			if (character == '\\') {
				position += escapeLength(position);
			} else if (WHITESPACE.indexOf(character) >= 0 || character == ')') {
				break;
			} else {
				position++;
			}
		}
		return new Span(start, position);
	}

	private Span readPhrase() {
		final var start = position;
		position++;
		while (position < query.length()) {
			final var character = query.charAt(position);
			if (character == '\\') {
				position += escapeLength(position);
			} else if (character == '"') {
				position++;
				return new Span(start, position);
			} else {
				position++;
			}
		}
		// Never closed, so it runs to the end, as the parser would have it
		return new Span(start, position);
	}

	private Span readRegex() {
		final var start = position;
		position++;
		while (position < query.length()) {
			final var character = query.charAt(position);
			if (character == '\\') {
				position += escapeLength(position);
			} else if (character == '/') {
				position++;
				return new Span(start, position);
			} else {
				position++;
			}
		}
		return new Span(start, position);
	}

	private void readRange() {
		final var opening = query.charAt(position);
		readBalanced(opening, opening == '[' ? ']' : '}');
	}

	private Span readBalanced(final char opening, final char closing) {
		final var start = position;
		var depth = 0;
		while (position < query.length()) {
			final var character = query.charAt(position);

			if (character == '\\') {
				position += escapeLength(position);
			} else if (character == '"') {
				readPhrase();
			} else {
				if (character == opening) {
					depth++;
				} else if (character == closing) {
					depth--;
					if (depth == 0) {
						position++;
						return new Span(start, position);
					}
				} else if (character == ':') {
					// A colon inside a group is a reference of its own, which the walk over the group body reads
					unaccountedColons++;
				}
				position++;
			}
		}
		return new Span(start, position);
	}

	/** What a value holds once a group or a pair of quotes around it is taken off. */
	private Span unwrap(final Span value) {
		if (value.end() - value.start() < 2) {
			return value;
		}
		final var first = query.charAt(value.start());
		final var last = query.charAt(value.end() - 1);
		final var wrapped = (first == '(' && last == ')') || (first == '"' && last == '"');
		return wrapped ? new Span(value.start() + 1, value.end() - 1) : value;
	}

	private int skipWhitespace(final int from) {
		var index = from;
		while (index < query.length() && WHITESPACE.indexOf(query.charAt(index)) >= 0) {
			index++;
		}
		return index;
	}

	/**
	 * How many characters an escape occupies: a unicode escape names its character with four digits, anything else
	 * escapes the one character after the backslash.
	 */
	private int escapeLength(final int at) {
		if (at + 1 < query.length() && (query.charAt(at + 1) == 'u' || query.charAt(at + 1) == 'U') && at + 5 < query.length() + 1 && isHex(at + 2)) {
			return 6;
		}
		return at + 1 < query.length() ? 2 : 1;
	}

	/**
	 * Whether the character may stand in a field name. Letters are taken as the language has them rather than as ASCII
	 * has them, since an errand is written in Swedish.
	 */
	private static boolean isNameCharacter(final char character) {
		return Character.isLetterOrDigit(character) || NAME_PUNCTUATION.indexOf(character) >= 0;
	}

	private boolean isHex(final int from) {
		if (from + 4 > query.length()) {
			return false;
		}
		for (var index = from; index < from + 4; index++) {
			if (Character.digit(query.charAt(index), 16) < 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The characters the parser is left with: an escape gives up the character it protects, and a unicode escape gives
	 * up the character it names, which is what makes {@code communications} the field {@code communications}.
	 */
	private String decode(final String text) {
		final var decoded = new StringBuilder(text.length());
		var index = 0;
		while (index < text.length()) {
			final var character = text.charAt(index);
			if (character != '\\' || index + 1 >= text.length()) {
				decoded.append(character);
				index++;
			} else if ((text.charAt(index + 1) == 'u' || text.charAt(index + 1) == 'U') && index + 6 <= text.length() && isHex(text, index + 2)) {
				decoded.append((char) Integer.parseInt(text.substring(index + 2, index + 6), 16));
				index += 6;
			} else {
				decoded.append(text.charAt(index + 1));
				index += 2;
			}
		}
		return decoded.toString();
	}

	private static boolean isHex(final String text, final int from) {
		for (var index = from; index < from + 4; index++) {
			if (Character.digit(text.charAt(index), 16) < 0) {
				return false;
			}
		}
		return true;
	}
}
