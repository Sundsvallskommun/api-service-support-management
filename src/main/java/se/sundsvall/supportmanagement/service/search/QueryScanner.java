package se.sundsvall.supportmanagement.service.search;

import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * Reads the fields a Lucene query string names, with the parser that OpenSearch reads it with.
 * <p>
 * What a search may look in is decided from the fields the query names, so reading the query wrongly is not a wrong
 * answer but an open door: a name we do not see is a field the index searches all the same. A copy of the grammar was
 * tried for this and kept being wrong about one shape or another - a quote ending a term, a regular expression inside
 * a group, a bracket within a range - so the query is read by the grammar itself instead. {@code query_string} is
 * Lucene's classic query parser with the leaf queries built by OpenSearch, so this is that parser with the leaf queries
 * built by nothing: every leaf the parser makes hands over the field it is made for, and that field is noted.
 * <p>
 * A query the parser cannot read is reported as not read rather than as naming nothing. The index would refuse it as
 * well, but a restricted search may not lean on that.
 */
final class QueryScanner {

	/** The field whose value is itself a field name. */
	private static final String EXISTS = "_exists_";

	/**
	 * What the parser gives a word that names no field, which no field of the index can be called: a field name holds no
	 * character below a space.
	 */
	private static final String FREE_TEXT = "\u0000";

	/** What every leaf is built as, since what the query would find is the index's business and not this one's. */
	private static final Query LEAF = new MatchAllDocsQuery();

	private QueryScanner() {}

	/**
	 * @param query the query as the client wrote it, null or blank giving an empty scan
	 */
	static Scan scan(final String query) {
		if (query == null || query.isBlank()) {
			return new Scan(List.of(), true);
		}

		final var parser = new FieldCollector();
		try {
			parser.parse(query);
		} catch (final ParseException | RuntimeException | StackOverflowError e) {
			// Not read to its end, so what it names is not known. The parser recurses into every group, so a client nesting
			// deep enough overflows the stack rather than failing to parse, and that is the same answer
			return new Scan(List.of(), false);
		}
		return new Scan(List.copyOf(parser.fields), true);
	}

	/**
	 * Whether a name stands for more than itself. A wildcard may name a field nobody enumerated, so only a route that is
	 * held to nothing may search one.
	 */
	static boolean isWildcard(final String name) {
		return name.indexOf('*') >= 0 || name.indexOf('?') >= 0;
	}

	/**
	 * What one query names.
	 *
	 * @param fields    the fields it names, escapes resolved, in the order the parser met them
	 * @param fullyRead whether the parser read it to its end, which a restricted search may not pass on without
	 */
	record Scan(List<String> fields, boolean fullyRead) {}

	/**
	 * The classic parser, noting the field of every leaf instead of building it. These are the methods OpenSearch's own
	 * parser overrides to build its leaves, so a field reaches one of them here exactly where it reaches the index there.
	 */
	private static final class FieldCollector extends QueryParser {

		private final List<String> fields = new ArrayList<>();

		private FieldCollector() {
			// The analyzer is never asked, since no leaf is built
			super(FREE_TEXT, new StandardAnalyzer());
			// As OpenSearch has it, where a term opening with a wildcard is a term and not an error
			setAllowLeadingWildcard(true);
		}

		@Override
		protected Query getFieldQuery(final String field, final String queryText, final boolean quoted) {
			return noteValueOrField(field, queryText);
		}

		@Override
		protected Query getFieldQuery(final String field, final String queryText, final int slop) {
			return noteValueOrField(field, queryText);
		}

		@Override
		protected Query getRangeQuery(final String field, final String part1, final String part2, final boolean startInclusive, final boolean endInclusive) {
			return note(field);
		}

		@Override
		protected Query getPrefixQuery(final String field, final String termStr) {
			return note(field);
		}

		@Override
		protected Query getWildcardQuery(final String field, final String termStr) {
			return note(field);
		}

		@Override
		protected Query getRegexpQuery(final String field, final String termStr) {
			return note(field);
		}

		@Override
		protected Query getFuzzyQuery(final String field, final String termStr, final float minSimilarity) {
			return note(field);
		}

		@Override
		protected Query getBooleanQuery(final List<BooleanClause> clauses) {
			// Nothing is built, so neither is the clause limit of a real query reached
			return LEAF;
		}

		/** The value of {@code _exists_} is the field it asks about, which OpenSearch searches for as such. */
		private Query noteValueOrField(final String field, final String queryText) {
			return note(EXISTS.equals(field) ? queryText : field);
		}

		private Query note(final String field) {
			if (!FREE_TEXT.equals(field)) {
				fields.add(field);
			}
			return LEAF;
		}
	}
}
