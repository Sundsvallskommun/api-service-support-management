package se.sundsvall.supportmanagement.service.search;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class QueryStringFieldsTest {

	@Test
	void fieldNames() {
		assertThat(QueryStringFields.fieldNames(null)).isEmpty();
		assertThat(QueryStringFields.fieldNames(" ")).isEmpty();
		assertThat(QueryStringFields.fieldNames("vatten läcka")).isEmpty();
		assertThat(QueryStringFields.fieldNames("title:vatten AND (status:new OR communications.messageBody:läcka)")).containsExactly("title", "status", "communications.messageBody");
		assertThat(QueryStringFields.fieldNames("created:[2025-01-01 TO 2025-12-31]")).containsExactly("created");
		// The value of _exists_ is a field
		assertThat(QueryStringFields.fieldNames("_exists_:communications.subject")).containsExactly("_exists_", "communications.subject");
		// Phrases are never fields, escapes are removed, a wildcard is kept
		assertThat(QueryStringFields.fieldNames("\"communications.subject:inside a phrase\" jsonParameters.\\*.regNo:abc description:communications.subject")).containsExactly("jsonParameters.*.regNo", "description");
		assertThat(QueryStringFields.fieldNames("communications\\:literal")).containsExactly("communications");
	}

	/**
	 * The parser binds a term to a field whatever whitespace stands between the name and the colon, so a name followed
	 * by a space is a name here too. Anything else would let a field be searched by putting a space in front of the
	 * colon, which {@code ErrandSearchIT} holds against a real OpenSearch.
	 */
	@Test
	void aNameIsANameWhateverStandsBetweenItAndTheColon() {
		assertThat(QueryStringFields.fieldNames("description :x")).containsExactly("description");
		assertThat(QueryStringFields.fieldNames("description : x")).containsExactly("description");
		assertThat(QueryStringFields.fieldNames("description\t:x")).containsExactly("description");
		assertThat(QueryStringFields.fieldNames("description\n:x")).containsExactly("description");
		assertThat(QueryStringFields.fieldNames("description     :     x")).containsExactly("description");
		// The parser passes over the ideographic space as well, which Java's own class of whitespace does not hold
		assertThat(QueryStringFields.fieldNames("description\u3000:x")).containsExactly("description");
		assertThat(QueryStringFields.fieldNames("_exists_\u3000:\u3000description")).containsExactly("_exists_", "description");
		assertThat(QueryStringFields.fieldNames("(description : x)")).containsExactly("description");
		assertThat(QueryStringFields.fieldNames("+description : x")).containsExactly("description");
		assertThat(QueryStringFields.fieldNames("* : x")).containsExactly("*");
		assertThat(QueryStringFields.fieldNames("_exists_ : communications.subject")).containsExactly("_exists_", "communications.subject");

		// A word standing on its own is still no name, whatever follows it
		assertThat(QueryStringFields.fieldNames("vatten läcka status : new")).containsExactly("status");
		assertThat(QueryStringFields.hasFreeTerms("description : x")).isFalse();
	}

	@Test
	void wildcard() {
		assertThat(QueryStringFields.isWildcard("jsonParameters.*.regNo")).isTrue();
		assertThat(QueryStringFields.isWildcard("comm?nications.subject")).isTrue();
		assertThat(QueryStringFields.isWildcard("communications.subject")).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"vatten", "title:x vatten", "title:(a OR b) läcka", "\"a phrase\" AND word", "-läcka", "berg*"
	})
	void freeTerms(final String query) {
		assertThat(QueryStringFields.hasFreeTerms(query)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"", " ", "title:x", "title:(a OR b) AND NOT status:new", "created:[2025-01-01 TO 2025-12-31]", "created:{* TO now-7d}", "title:\"a phrase\"", "_exists_:assignedUserId", "+title:x -status:closed"
	})
	void fieldedOnly(final String query) {
		assertThat(QueryStringFields.hasFreeTerms(query)).isFalse();
	}

	/**
	 * The query comes from the client, so a long one must cost what its length costs and nothing more. Written as a
	 * choice repeated per character, the phrase pattern walked the engine into the stack for a few thousand characters
	 * after an unclosed quote.
	 */
	@Test
	void aLongQueryIsReadWithoutWalkingTheStack() {
		final var unclosed = "\"" + "a".repeat(100_000);
		final var unterminatedField = "title:" + "a".repeat(100_000);

		assertThat(QueryStringFields.fieldNames(unclosed)).isEmpty();
		assertThat(QueryStringFields.hasFreeTerms(unclosed)).isTrue();
		assertThat(QueryStringFields.fieldNames(unterminatedField)).containsExactly("title");
		assertThat(QueryStringFields.hasFreeTerms(unterminatedField)).isFalse();
	}

	/**
	 * A query is held to a length the endpoint accepts, and reading one must cost that length rather than its square.
	 * Brackets never closed are the shape that cost the square: each of them was followed to the end of the query.
	 * Every one of them is a field carrying a value of its own, so nothing without a field is left.
	 */
	@Test
	void aQueryOfUnclosedBracketsIsReadInItsLength() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			assertThat(QueryStringFields.hasFreeTerms("a:( ".repeat(25_000))).isFalse();
			assertThat(QueryStringFields.hasFreeTerms("a:[ ".repeat(25_000))).isFalse();
			assertThat(QueryStringFields.hasFreeTerms("a:{ ".repeat(25_000))).isFalse();
		});
	}

	/**
	 * A value carrying brackets of its own ends at the first space, as it ended at the first closing bracket before:
	 * what is left of it is words, and words without a field are what this answers.
	 */
	@Test
	void aGroupWithinAGroupCountsAsWords() {
		assertThat(QueryStringFields.hasFreeTerms("title:((a OR b) AND c)")).isTrue();
	}
}
