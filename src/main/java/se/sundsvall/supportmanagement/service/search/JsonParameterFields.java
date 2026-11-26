package se.sundsvall.supportmanagement.service.search;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.lucene.queryparser.charstream.FastCharStream;
import org.apache.lucene.queryparser.classic.QueryParserTokenManager;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.queryparser.classic.TokenMgrError;
import se.sundsvall.supportmanagement.model.ErrandIndex;

import static org.apache.lucene.queryparser.classic.QueryParserConstants.CARAT;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.COLON;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.EOF;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.FUZZY_SLOP;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.LPAREN;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.NUMBER;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.RANGEEX_END;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.RANGEEX_START;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.RANGEIN_END;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.RANGEIN_START;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.RPAREN;
import static org.apache.lucene.queryparser.classic.QueryParserConstants.TERM;
import static se.sundsvall.supportmanagement.integration.db.search.JsonParametersBinder.VALUE;

/**
 * Turns the fields a query names under the JSON parameters into the fields the index holds them in.
 * <p>
 * Every value of a JSON parameter is written one step further down than its path, under
 * {@link se.sundsvall.supportmanagement.integration.db.search.JsonParametersBinder#VALUE}, so that a path never is a
 * value in one errand and an object in another (see the binder). A client names the path as the JSON has it, and is
 * answered as before: {@code jsonParameters.vehicle.owner} is looked for in {@code jsonParameters.vehicle.owner._v},
 * and
 * its exact twin {@code jsonParameters.vehicle.owner.raw} in {@code jsonParameters.vehicle.owner._v.raw}.
 * <p>
 * Only names are changed, never what is looked for in them, and a name is found the way OpenSearch finds it: by the
 * tokens of the classic query parser, a term followed by a colon. A name ending in a wildcard already reaches what is
 * below it and is left be, as is the field {@code _exists_} asks about, which is an object either way - unless it is
 * the exact twin of a path, which is below the value. A name with a
 * wildcard elsewhere that does not start in the JSON parameters may still reach into them -
 * {@code *.supplier} - and so asks both for what it names and for the value below it.
 * <p>
 * The query is changed after it has been checked against what the caller may search: what is checked is what the client
 * wrote, the key still being the segment after {@code jsonParameters}. A query the tokens cannot be read from is left
 * as it is, and refused by the index as it would have been.
 */
final class JsonParameterFields {

	private static final String RAW = ".raw";
	private static final String EXISTS = "_exists_";
	private static final String LEAF = "." + VALUE;
	private static final Set<Integer> BOOST_OR_SLOP = Set.of(CARAT, NUMBER, FUZZY_SLOP);

	private JsonParameterFields() {}

	/**
	 * @param  query the query as the client wrote it
	 * @return       the query naming the fields the index holds
	 */
	static String rewrite(final String query) {
		if (query == null || !query.contains(ErrandIndex.JSON_PARAMETERS) && query.indexOf('*') < 0 && query.indexOf('?') < 0) {
			return query;
		}

		final List<Token> tokens;
		try {
			tokens = tokens(query);
		} catch (final TokenMgrError e) {
			return query;
		}

		final var rewriting = new Rewriting(query, tokens);
		var i = 0;
		while (i < tokens.size()) {
			i = rewriting.at(i);
		}
		return rewriting.result();
	}

	/** A query on its way through, copied over as far as its tokens have been looked at. */
	private static final class Rewriting {

		private final String query;
		private final List<Token> tokens;
		private final StringBuilder rewritten = new StringBuilder();
		private int copiedUpTo;

		Rewriting(final String query, final List<Token> tokens) {
			this.query = query;
			this.tokens = tokens;
		}

		/** Rewrites what the token at sent in index begins, if anything, and says where to go on from. */
		int at(final int i) {
			if (!isField(tokens, i)) {
				return i + 1;
			}

			final var name = tokens.get(i);
			if (EXISTS.equals(name.image)) {
				return existsAt(i);
			}

			final var names = namesFor(name.image);
			if (names.size() == 1) {
				replace(name.beginColumn, name.endColumn, names.getFirst());
				return i + 1;
			}

			// Both names, each with what was asked of the one: the value runs from after the colon to its end
			final var last = endOfValue(tokens, i + 2);
			if (last < 0) {
				return i + 1;
			}
			final var value = query.substring(tokens.get(i + 2).beginColumn, tokens.get(last).endColumn);
			replace(name.beginColumn, tokens.get(last).endColumn, "(%s:%s OR %s:%s)".formatted(names.get(0), value, names.get(1), value));
			return last + 1;
		}

		/**
		 * The field {@code _exists_} asks about is an object below a path of the JSON parameters, and found as it is. Its
		 * exact twin is not, as {@code .raw} is below the value, and is asked about there.
		 */
		private int existsAt(final int i) {
			final var field = tokens.get(i + 2);
			if (field.kind == TERM) {
				final var bare = field.image.replace("\\", "");
				if (bare.endsWith(RAW) && isUnderJsonParameters(bare)) {
					replace(field.beginColumn, field.endColumn, toValue(field.image));
				}
			}
			return i + 3;
		}

		private void replace(final int from, final int to, final String text) {
			if (!text.equals(query.substring(from, to))) {
				rewritten.append(query, copiedUpTo, from).append(text);
				copiedUpTo = to;
			}
		}

		String result() {
			return copiedUpTo == 0 ? query : rewritten.append(query.substring(copiedUpTo)).toString();
		}
	}

	/**
	 * What a name is looked for in: the value below a path of the JSON parameters, the name itself and the value below
	 * it for a wildcard that may reach into them, and the name itself for anything else.
	 */
	static List<String> namesFor(final String name) {
		final var bare = name.replace("\\", "");
		if (bare.endsWith("*")) {
			return List.of(name);
		}
		if (isUnderJsonParameters(bare)) {
			return List.of(toValue(name));
		}
		if (QueryScanner.isWildcard(bare)) {
			return List.of(name, toValue(name));
		}
		return List.of(name);
	}

	private static String toValue(final String name) {
		return name.endsWith(RAW) ? name.substring(0, name.length() - RAW.length()) + LEAF + RAW : name + LEAF;
	}

	/** A name with a segment after a {@code jsonParameters} segment, be it of the errand or of what hangs off it. */
	private static boolean isUnderJsonParameters(final String bare) {
		final var segments = bare.split("\\.", -1);
		for (var i = 0; i < segments.length - 1; i++) {
			if (ErrandIndex.JSON_PARAMETERS.equals(segments[i])) {
				return true;
			}
		}
		return false;
	}

	private static boolean isField(final List<Token> tokens, final int i) {
		return tokens.get(i).kind == TERM && i + 1 < tokens.size() && tokens.get(i + 1).kind == COLON && i + 2 < tokens.size();
	}

	/**
	 * The last token of the value starting at sent in token: a group up to the parenthesis closing it, a range up to its
	 * end, or a single term, with a boost or a fuzziness after any of them. Negative when the value does not end.
	 */
	private static int endOfValue(final List<Token> tokens, final int start) {
		final var kind = tokens.get(start).kind;
		final int end;
		if (kind == LPAREN) {
			end = endOfGroup(tokens, start);
		} else if (kind == RANGEIN_START || kind == RANGEEX_START) {
			end = endOfRange(tokens, start);
		} else {
			end = start;
		}
		return end < 0 ? end : withBoostOrSlop(tokens, end);
	}

	/** The parenthesis closing the group opened at sent in token, negative when there is none. */
	private static int endOfGroup(final List<Token> tokens, final int start) {
		var depth = 0;
		for (var end = start; end < tokens.size(); end++) {
			depth += depthChange(tokens.get(end).kind);
			if (depth == 0) {
				return end;
			}
		}
		return -1;
	}

	private static int depthChange(final int kind) {
		if (kind == LPAREN) {
			return 1;
		}
		return kind == RPAREN ? -1 : 0;
	}

	/** The end of the range opened at sent in token, negative when there is none. */
	private static int endOfRange(final List<Token> tokens, final int start) {
		for (var end = start; end < tokens.size(); end++) {
			if (tokens.get(end).kind == RANGEIN_END || tokens.get(end).kind == RANGEEX_END) {
				return end;
			}
		}
		return -1;
	}

	/** The last of the boost and fuzziness tokens following sent in token, or the token itself. */
	private static int withBoostOrSlop(final List<Token> tokens, final int end) {
		var last = end;
		while (last + 1 < tokens.size() && BOOST_OR_SLOP.contains(tokens.get(last + 1).kind)) {
			last++;
		}
		return last;
	}

	/**
	 * The tokens of the query. Over a single string the columns of a token are offsets in it: where it begins, and where
	 * the character after it is.
	 */
	private static List<Token> tokens(final String query) {
		final var tokenManager = new QueryParserTokenManager(new FastCharStream(new StringReader(query)));
		final var tokens = new ArrayList<Token>();
		for (var token = tokenManager.getNextToken(); token.kind != EOF; token = tokenManager.getNextToken()) {
			tokens.add(token);
		}
		return tokens;
	}
}
