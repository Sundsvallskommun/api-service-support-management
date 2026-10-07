package se.sundsvall.supportmanagement.service.search;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * The shapes a query comes in, and the fields each of them names. Every case that names a field is one the index would
 * search that field for, so a case reading differently here than OpenSearch reads it is a field searched without being
 * allowed.
 */
class QueryScannerTest {

	/**
	 * Nothing at all, words, a phrase and an escaped colon name no field.
	 */
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		" ", "\u3000", "vatten läcka", "\"communications.subject:inside a phrase\"", "title\\:x", "a b a b a b"
	})
	void aQueryNamingNoFieldNamesNothing(final String query) {
		final var scan = QueryScanner.scan(query);

		assertThat(scan.fields()).isEmpty();
		assertThat(scan.fullyRead()).isTrue();
	}

	@Test
	void aFieldAndItsValue() {
		assertThat(QueryScanner.scan("title:vatten").fields()).containsExactly("title");
	}

	@Test
	void aFieldBesideAWord() {
		assertThat(QueryScanner.scan("title:x vatten").fields()).containsExactly("title");
	}

	/**
	 * The parser passes over whitespace between a name and its colon, the ideographic space among it, so a space in
	 * front of the colon names the very same field.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"description:x", "description :x", "description : x", "description\t:x", "description\n:x", "description\u3000:x", "description     :     x"
	})
	void aNameIsANameWhateverStandsBetweenItAndTheColon(final String query) {
		assertThat(QueryScanner.scan(query).fields()).containsExactly("description");
	}

	/**
	 * A minus is the grammar's own, not the first letter of a name: the field searched is the one behind it.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"-communications.subject:secret", "+communications.subject:secret", "!communications.subject:secret", "title:x -communications.subject:secret", "(communications.subject:secret)"
	})
	void anOperatorInFrontOfANameIsNoPartOfIt(final String query) {
		assertThat(QueryScanner.scan(query).fields()).contains("communications.subject");
	}

	/**
	 * An escape gives up the character it protects, and a unicode escape the character it names, before anything looks
	 * at the name.
	 */
	@Test
	void escapesAreResolvedBeforeTheNameIsRead() {
		assertThat(QueryScanner.scan("\\u0063ommunications.subject:secret").fields()).containsExactly("communications.subject");
		assertThat(QueryScanner.scan("communications.\\u0073ubject:secret").fields()).containsExactly("communications.subject");
		assertThat(QueryScanner.scan("jsonParameters.\\*.regNo:abc").fields()).containsExactly("jsonParameters.*.regNo");
	}

	/**
	 * A quote that is escaped opens no phrase, so what follows it is read as the parser reads it rather than hidden.
	 */
	@Test
	void anEscapedQuoteHidesNothing() {
		assertThat(QueryScanner.scan("x\\\" communications.subject:secret \"y\"").fields()).containsExactly("communications.subject");
	}

	/**
	 * A quote inside a regular expression is an ordinary character of it, so the fielded term after it is still read.
	 */
	@Test
	void aRegularExpressionHidesNothingEither() {
		assertThat(QueryScanner.scan("title:/a\"/ communications.subject:x").fields()).containsExactly("title", "communications.subject");
	}

	/**
	 * The shapes a copy of the grammar read as naming the status alone: a quote or a slash ends a term, a group holds
	 * regular expressions, and a bracket inside a range is part of its bound. The parser reads the description out of
	 * every one of them, so this must too.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"status:x\"y z\" description:secret",
		"status:x/y /description:secret",
		"status:(/\"/ description:secret /\"/)",
		"status:[[a TO b] description:secret /]/"
	})
	void whatEndsATermIsWhatTheParserEndsItAt(final String query) {
		final var scan = QueryScanner.scan(query);

		assertThat(scan.fullyRead()).isTrue();
		assertThat(scan.fields()).contains("status", "description");
	}

	/**
	 * The value of _exists_ is a field name, bare, in a group or in quotes.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"_exists_:communications.subject", "_exists_:(communications.subject)", "_exists_:\"communications.subject\"", "_exists_ : communications.subject"
	})
	void theValueOfExistsIsAField(final String query) {
		assertThat(QueryScanner.scan(query).fields()).containsExactly("communications.subject");
	}

	/**
	 * The object's own name is a field of the index too: asking whether it exists asks whether the errand holds any of
	 * what hangs under it.
	 */
	@Test
	void theNameOfAnObjectIsAFieldOfItsOwn() {
		assertThat(QueryScanner.scan("_exists_:communications").fields()).containsExactly("communications");
	}

	/**
	 * Every kind of leaf the parser builds hands over its field: a term, a phrase, a range, a prefix, a wildcard, a
	 * regular expression and a fuzzy term.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"title:x", "title:\"a b\"", "title:\"a b\"~2", "title:[a TO b]", "title:{a TO *}", "title:>=a", "title:ab*", "title:a?c", "title:*", "title:/a.c/", "title:abc~", "title:(a OR b)"
	})
	void everyLeafHandsOverItsField(final String query) {
		final var scan = QueryScanner.scan(query);

		assertThat(scan.fullyRead()).isTrue();
		assertThat(scan.fields()).isNotEmpty().containsOnly("title");
	}

	@Test
	void aRangeIsAValue() {
		assertThat(QueryScanner.scan("created:[2025-01-01 TO 2025-12-31]").fields()).containsExactly("created");
	}

	/**
	 * A range may be closed on one end and open on the other, and the colons of a timestamp belong to the bound they
	 * stand in.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"created:[2025-01-01T00:00:00Z TO 2025-12-31T23:59:59Z] status:new", "created:[2025-01-01 TO 2025-12-31} status:new"
	})
	void aRangeEndsWhereTheParserEndsIt(final String query) {
		final var scan = QueryScanner.scan(query);

		assertThat(scan.fullyRead()).isTrue();
		assertThat(scan.fields()).containsExactly("created", "status");
	}

	@Test
	void aFieldInFrontOfAGroupIsTheFieldOfEveryTermInIt() {
		assertThat(QueryScanner.scan("title:(a OR b) läcka").fields()).containsExactly("title", "title");
	}

	/**
	 * What the parser cannot read names nothing anyone knows of, so it is reported rather than passed for understood.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		":x",
		"title:a :b",
		"title:a:communications.subject:secret",
		"errandNumber:x:decisions.justification:avslag",
		"title:(a OR b status:new",
		"\"never closed"
	})
	void aQueryTheParserCannotReadIsReported(final String query) {
		final var scan = QueryScanner.scan(query);

		assertThat(scan.fullyRead()).isFalse();
		assertThat(scan.fields()).isEmpty();
	}

	@Test
	void aWildcardInANameStandsForMoreThanItself() {
		assertThat(QueryScanner.isWildcard("jsonParameters.*.regNo")).isTrue();
		assertThat(QueryScanner.isWildcard("titl?")).isTrue();
		assertThat(QueryScanner.isWildcard("title")).isFalse();
	}

	/**
	 * The query comes from the client, so a long one must cost what its length costs, and one nested deeper than the
	 * parser can follow is not read rather than failing the request.
	 */
	@Test
	void aLongQueryIsReadInItsLength() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			assertThat(QueryScanner.scan("title:" + "a".repeat(200_000)).fields()).containsExactly("title");
			assertThat(QueryScanner.scan("a b ".repeat(50_000)).fullyRead()).isTrue();
			assertThat(QueryScanner.scan("a:( ".repeat(50_000)).fullyRead()).isFalse();
		});
	}
}
