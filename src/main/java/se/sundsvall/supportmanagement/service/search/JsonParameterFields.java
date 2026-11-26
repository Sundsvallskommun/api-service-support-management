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
 * below it and is left be, as is the field {@code _exists_} asks about, which is an object either way. A name with a
 * wildcard elsewhere that does not start in the JSON parameters may still reach into them -
 * {@code *.supplier} - and so asks both for what it names and for the value below it.
 * <p>
 * The query is changed after it has been checked against what the caller may search: what is checked is what the client
 * wrote, the key still being the segment after {@code jsonParameters}. A query the tokens cannot be read from is left
 * as it is, and refused by the index as it would have been.
 */
final class JsonParameterFields {

	private static final String RAW = ".raw";
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

		final var rewritten = new StringBuilder();
		var copiedUpTo = 0;
		for (var i = 0; i < tokens.size(); i++) {
			if (!isField(tokens, i)) {
				continue;
			}

			final var name = tokens.get(i);
			final var names = namesFor(name.image);
			if (names.size() == 1 && names.getFirst().equals(name.image)) {
				continue;
			}

			rewritten.append(query, copiedUpTo, name.beginColumn);
			if (names.size() == 1) {
				rewritten.append(names.getFirst());
				copiedUpTo = name.endColumn;
				continue;
			}

			// Both names, each with what was asked of the one: the value runs from after the colon to its end
			final var last = endOfValue(tokens, i + 2);
			if (last < 0) {
				rewritten.append(name.image);
				copiedUpTo = name.endColumn;
				continue;
			}
			final var value = query.substring(tokens.get(i + 2).beginColumn, tokens.get(last).endColumn);
			rewritten.append('(').append(names.get(0)).append(':').append(value).append(" OR ").append(names.get(1)).append(':').append(value).append(')');
			copiedUpTo = tokens.get(last).endColumn;
			i = last;
		}

		return copiedUpTo == 0 ? query : rewritten.append(query.substring(copiedUpTo)).toString();
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
		var end = start;
		final var kind = tokens.get(start).kind;
		if (kind == LPAREN) {
			var depth = 0;
			for (; end < tokens.size(); end++) {
				depth += tokens.get(end).kind == LPAREN ? 1 : tokens.get(end).kind == RPAREN ? -1 : 0;
				if (depth == 0) {
					break;
				}
			}
		} else if (kind == RANGEIN_START || kind == RANGEEX_START) {
			while (end < tokens.size() && tokens.get(end).kind != RANGEIN_END && tokens.get(end).kind != RANGEEX_END) {
				end++;
			}
		}
		if (end >= tokens.size()) {
			return -1;
		}
		while (end + 1 < tokens.size() && BOOST_OR_SLOP.contains(tokens.get(end + 1).kind)) {
			end++;
		}
		return end;
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
