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

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		" ", "　"
	})
	void nothingIsNamedByNothing(final String query) {
		final var scan = QueryScanner.scan(query);

		assertThat(scan.fields()).isEmpty();
		assertThat(scan.hasFreeTerms()).isFalse();
		assertThat(scan.isFullyRead()).isTrue();
	}

	@Test
	void aFieldAndItsValue() {
		final var scan = QueryScanner.scan("title:vatten");

		assertThat(scan.fieldNames()).containsExactly("title");
		assertThat(scan.hasFreeTerms()).isFalse();
	}

	@Test
	void wordsNamingNoField() {
		final var scan = QueryScanner.scan("vatten läcka");

		assertThat(scan.fields()).isEmpty();
		assertThat(scan.freeTerms()).hasSize(2);
	}

	@Test
	void aFieldBesideAWord() {
		final var scan = QueryScanner.scan("title:x vatten");

		assertThat(scan.fieldNames()).containsExactly("title");
		assertThat(scan.hasFreeTerms()).isTrue();
	}

	/**
	 * The parser passes over whitespace between a name and its colon, the ideographic space among it, so a space in
	 * front of the colon names the very same field.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"description:x", "description :x", "description : x", "description\t:x", "description\n:x", "description　:x", "description     :     x"
	})
	void aNameIsANameWhateverStandsBetweenItAndTheColon(final String query) {
		assertThat(QueryScanner.scan(query).fieldNames()).containsExactly("description");
		assertThat(QueryScanner.scan(query).hasFreeTerms()).isFalse();
	}

	/**
	 * A minus is the grammar's own, not the first letter of a name: the field searched is the one behind it.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"-communications.subject:secret", "+communications.subject:secret", "!communications.subject:secret", "title:x -communications.subject:secret", "(communications.subject:secret)"
	})
	void anOperatorInFrontOfANameIsNoPartOfIt(final String query) {
		assertThat(QueryScanner.scan(query).fieldNames()).contains("communications.subject");
	}

	/**
	 * An escape gives up the character it protects, and a unicode escape the character it names, before anything looks
	 * at the name.
	 */
	@Test
	void escapesAreResolvedBeforeTheNameIsRead() {
		assertThat(QueryScanner.scan("\\u0063ommunications.subject:secret").fieldNames()).containsExactly("communications.subject");
		assertThat(QueryScanner.scan("communications.\\u0073ubject:secret").fieldNames()).containsExactly("communications.subject");
		assertThat(QueryScanner.scan("jsonParameters.\\*.regNo:abc").fieldNames()).containsExactly("jsonParameters.*.regNo");
	}

	/**
	 * A quote that is escaped opens no phrase, so what follows it is read as the parser reads it rather than hidden.
	 */
	@Test
	void anEscapedQuoteHidesNothing() {
		final var scan = QueryScanner.scan("x\\\" communications.subject:secret \"y\"");

		assertThat(scan.fieldNames()).containsExactly("communications.subject");
	}

	@Test
	void aPhraseNamesNoField() {
		final var scan = QueryScanner.scan("\"communications.subject:inside a phrase\"");

		assertThat(scan.fields()).isEmpty();
		assertThat(scan.hasFreeTerms()).isTrue();
	}

	/**
	 * A quote inside a regular expression is an ordinary character of it, so the fielded term after it is still read.
	 */
	@Test
	void aRegularExpressionHidesNothingEither() {
		final var scan = QueryScanner.scan("title:/a\"/ communications.subject:x");

		assertThat(scan.fieldNames()).containsExactly("title", "communications.subject");
	}

	/**
	 * The value of _exists_ is a field name, bare, in a group or in quotes.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"_exists_:communications.subject", "_exists_:(communications.subject)", "_exists_:\"communications.subject\"", "_exists_ : communications.subject"
	})
	void theValueOfExistsIsAField(final String query) {
		assertThat(QueryScanner.scan(query).fieldNames()).containsExactly("communications.subject");
	}

	/**
	 * The object's own name is a field of the index too: asking whether it exists asks whether the errand holds any of
	 * what hangs under it.
	 */
	@Test
	void theNameOfAnObjectIsAFieldOfItsOwn() {
		assertThat(QueryScanner.scan("_exists_:communications").fieldNames()).containsExactly("communications");
	}

	@Test
	void aRangeIsAValueAndNoWordOfItsOwn() {
		final var scan = QueryScanner.scan("created:[2025-01-01 TO 2025-12-31]");

		assertThat(scan.fieldNames()).containsExactly("created");
		assertThat(scan.hasFreeTerms()).isFalse();
	}

	@Test
	void aGroupIsAValueAndTheWordAfterItIsNot() {
		final var scan = QueryScanner.scan("title:(a OR b) läcka");

		assertThat(scan.fieldNames()).containsExactly("title");
		assertThat(scan.freeTerms()).hasSize(1);
	}

	@Test
	void operatorsAreNoWordsOfTheirOwn() {
		assertThat(QueryScanner.scan("title:a AND NOT status:b").hasFreeTerms()).isFalse();
		assertThat(QueryScanner.scan("created:{* TO now-7d}").hasFreeTerms()).isFalse();
	}

	/**
	 * A colon this cannot read as part of a reference is a fielded term it does not understand. Saying so is what keeps
	 * a shape nobody thought of from reaching the index unchecked.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		":x", "title:a :b"
	})
	void aColonThatCouldNotBeReadIsReported(final String query) {
		assertThat(QueryScanner.scan(query).isFullyRead()).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"title:x", "vatten", "_exists_:title", "title:\"a:b\"", "title:/a:b/", "created:[2025-01-01 TO 2025-12-31]"
	})
	void aQueryThisUnderstandsSaysSo(final String query) {
		assertThat(QueryScanner.scan(query).isFullyRead()).isTrue();
	}

	@Test
	void theSpanOfANameIsWhereItStands() {
		final var scan = QueryScanner.scan("title:x -communications.subject:secret");
		final var reference = scan.fields().get(1);

		assertThat("title:x -communications.subject:secret".substring(reference.inName().start(), reference.inName().end())).isEqualTo("communications.subject");
		// The term the name belongs to, the minus in front of it left out, which is what a rewrite of it replaces
		assertThat("title:x -communications.subject:secret".substring(reference.inTerm().start(), reference.inTerm().end())).isEqualTo("communications.subject:secret");
	}

	/**
	 * The query comes from the client, so a long one must cost what its length costs.
	 */
	@Test
	void aLongQueryIsReadInItsLength() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			assertThat(QueryScanner.scan("\"" + "a".repeat(200_000)).fields()).isEmpty();
			assertThat(QueryScanner.scan("title:" + "a".repeat(200_000)).fieldNames()).containsExactly("title");
			assertThat(QueryScanner.scan("a:( ".repeat(50_000)).fields()).isNotEmpty();
			assertThat(QueryScanner.scan("a b ".repeat(50_000)).freeTerms()).isNotEmpty();
		});
	}
}
