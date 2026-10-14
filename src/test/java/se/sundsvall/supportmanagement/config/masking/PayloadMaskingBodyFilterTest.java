package se.sundsvall.supportmanagement.config.masking;

import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

class PayloadMaskingBodyFilterTest {

	private static final String JSON = "application/json";

	private static PayloadMaskingBodyFilter filter(final String... keep) {
		return new PayloadMaskingBodyFilter(new PayloadMaskingProperties(Set.of(keep), "[masked]", 262144));
	}

	@Test
	void keepsWhatIsListedAndMasksTheRest() {
		final var result = filter("$..id", "$..status").filter(JSON,
			"""
				{"id":"abc","status":"ONGOING","title":"Something broken","description":"Free text"}""");

		assertThat(result).isEqualTo(
			"""
				{"id":"abc","status":"ONGOING","title":"[masked]","description":"[masked]"}""");
	}

	@Test
	void keepsEveryPropertyNameAndEveryNonTextValue() {
		final var result = filter().filter(JSON,
			"""
				{"fileSize":274,"internal":false,"viewed":null,"fileName":"anna.pdf"}""");

		assertThat(result).isEqualTo(
			"""
				{"fileSize":274,"internal":false,"viewed":null,"fileName":"[masked]"}""");
	}

	@Test
	void masksNestedObjectsAndArraysWithoutLosingTheirShape() {
		final var result = filter("$..id", "$..type").filter(JSON,
			"""
				{"stakeholders":[{"id":"1","firstName":"Anna","contactChannels":[{"type":"Email","value":"a@b.c"}]}]}""");

		assertThat(result).isEqualTo(
			"""
				{"stakeholders":[{"id":"1","firstName":"[masked]","contactChannels":[{"type":"Email","value":"[masked]"}]}]}""");
	}

	@Test
	void judgesStringsInAnArrayByTheFieldTheArrayBelongsTo() {
		final var result = filter("$..relationIds").filter(JSON,
			"""
				{"recipients":["a@b.c","d@e.f"],"relationIds":["REL-1"]}""");

		assertThat(result).isEqualTo(
			"""
				{"recipients":["[masked]","[masked]"],"relationIds":["REL-1"]}""");
	}

	@Test
	void keepsAFieldOnlyUnderTheParentItIsListedWith() {
		final var filter = filter("$..key", "$..metadata.value");

		assertThat(filter.filter(JSON, """
			{"metadata":[{"key":"Status","value":"STATUS-3"}]}"""))
			.isEqualTo("""
				{"metadata":[{"key":"Status","value":"STATUS-3"}]}""");

		assertThat(filter.filter(JSON, """
			{"operations":[{"op":"replace","path":"/title","value":"It is my birthday"}]}"""))
			.isEqualTo("""
				{"operations":[{"op":"[masked]","path":"[masked]","value":"[masked]"}]}""");
	}

	@Test
	void masksThePageRowsWithoutReplacingThePageItself() {
		final var result = filter("$..id").filter(JSON,
			"""
				{"content":[{"id":"1","content":"Bifogar en fil"}],"totalElements":1}""");

		assertThat(result).isEqualTo(
			"""
				{"content":[{"id":"1","content":"[masked]"}],"totalElements":1}""");
	}

	@Test
	void leavesABodyThatIsNotJsonAlone() {
		assertThat(filter().filter("text/plain", "Testing! :)")).isEqualTo("Testing! :)");
		assertThat(filter().filter("application/xml", "<a>b</a>")).isEqualTo("<a>b</a>");
		assertThat(filter().filter(null, "{\"a\":\"b\"}")).isEqualTo("{\"a\":\"b\"}");
	}

	@Test
	void masksAProblemResponseToo() {
		assertThat(filter("$..status").filter("application/problem+json",
			"""
				{"status":404,"title":"Not Found","detail":"No errand with id 1"}"""))
			.isEqualTo("""
				{"status":404,"title":"[masked]","detail":"[masked]"}""");
	}

	@Test
	void passesOnAnEmptyBody() {
		assertThat(filter().filter(JSON, null)).isNull();
		assertThat(filter().filter(JSON, "  ")).isEqualTo("  ");
	}

	@Test
	void withholdsABodyItCannotRead() {
		assertThat(filter().filter(JSON, "{not json at all")).isEqualTo("{\"masked\":\"unreadable\"}");
	}

	@Test
	void withholdsABodyTooLargeToReadWithoutReadingIt() {
		final var attachment = "{\"base64EncodedString\":\"" + "A".repeat(300_000) + "\"}";

		final var result = new PayloadMaskingBodyFilter(new PayloadMaskingProperties(Set.of(), "[masked]", 262144))
			.filter(JSON, attachment);

		assertThat(result)
			.isEqualTo("{\"masked\":\"too large\",\"characters\":" + attachment.length() + "}")
			.doesNotContain("AAAA");
	}

	/**
	 * A replacement is embedded into the log entry as it is, so one that is not JSON costs the entry its fields
	 * wherever the log server reads it as JSON.
	 */
	@Test
	void replacesABodyWithSomethingThatIsStillJson() {
		final var filter = filter();

		assertThat(filter.filter(JSON, "{not json at all")).satisfies(PayloadMaskingBodyFilterTest::isJson);
		assertThat(new PayloadMaskingBodyFilter(new PayloadMaskingProperties(Set.of(), "[masked]", 8))
			.filter(JSON, "{\"a\":\"bbbbbbbbbbbbbbbb\"}")).satisfies(PayloadMaskingBodyFilterTest::isJson);
	}

	@Test
	void keepsAFieldOnlyWhereAnAnchoredPathSaysSo() {
		// $[*].name is a metadata list; $.sender.name is the name of whoever sent an e-mail
		final var filter = filter("$[*].name", "$..id");

		assertThat(filter.filter(JSON, """
			[{"id":"1","name":"STATUS-1"},{"id":"2","name":"STATUS-2"}]"""))
			.isEqualTo("""
				[{"id":"1","name":"STATUS-1"},{"id":"2","name":"STATUS-2"}]""");

		assertThat(filter.filter(JSON, """
			{"sender":{"name":"Bender Bending Rodriguez","address":"bender@rodriguez.com"}}"""))
			.isEqualTo("""
				{"sender":{"name":"[masked]","address":"[masked]"}}""");
	}

	@Test
	void treatsArrayNestingAsTransparentSoTheElementMarkerIsOptional() {
		final var withMarker = filter("$.content[*].title").filter(JSON, """
			{"content":[{"title":"Kept","description":"Masked"}]}""");
		final var withoutMarker = filter("$.content.title").filter(JSON, """
			{"content":[{"title":"Kept","description":"Masked"}]}""");

		assertThat(withMarker).isEqualTo(withoutMarker).isEqualTo("""
			{"content":[{"title":"Kept","description":"[masked]"}]}""");
	}

	@Test
	void anchorsAPathAtTheRootAndKeepsADescendantAtAnyDepth() {
		assertThat(filter("$.title").filter(JSON, """
			{"title":"Kept","errand":{"title":"Masked"}}"""))
			.isEqualTo("""
				{"title":"Kept","errand":{"title":"[masked]"}}""");

		assertThat(filter("$..title").filter(JSON, """
			{"title":"Kept","errand":{"title":"Kept"}}"""))
			.isEqualTo("""
				{"title":"Kept","errand":{"title":"Kept"}}""");
	}

	@Test
	void keepsAnyOneFieldForAWildcardSegment() {
		assertThat(filter("$.*.id").filter(JSON, """
			{"a":{"id":"1","x":"m"},"b":{"id":"2"},"c":{"d":{"id":"3"}}}"""))
			.isEqualTo("""
				{"a":{"id":"1","x":"[masked]"},"b":{"id":"2"},"c":{"d":{"id":"[masked]"}}}""");
	}

	@Test
	void refusesAPathItCannotRead() {
		assertThatIllegalArgumentException().isThrownBy(() -> filter("$.a[?(@.b=='c')].d"))
			.withMessageContaining("predicate");
		assertThatIllegalArgumentException().isThrownBy(() -> filter("$.a[0].b"))
			.withMessageContaining("predicate");
		assertThatIllegalArgumentException().isThrownBy(() -> filter("$.."))
			.withMessageContaining("empty field");
		assertThatIllegalArgumentException().isThrownBy(() -> filter("$a.b"))
			.withMessageContaining("must continue with");
		assertThatIllegalArgumentException().isThrownBy(() -> filter("title"))
			.withMessageContaining("is not a path");
	}

	@Test
	void withholdsABodyThatIsNothingButAString() {
		// No field names it, so nothing keeps it. Quoted, so that what replaces it is still a JSON document
		assertThat(filter("$..title").filter(JSON, "\"Anna Andersson, Storgatan 1\""))
			.isEqualTo("\"[masked]\"")
			.satisfies(PayloadMaskingBodyFilterTest::isJson);

		// A number is kept wherever it is found, at the root as anywhere else
		assertThat(filter().filter(JSON, "199001011234")).isEqualTo("199001011234");
	}

	@Test
	void withholdsABodyNestedTooDeeplyToRead() {
		final var deep = "{\"a\":".repeat(6000) + "\"x\"" + "}".repeat(6000);

		assertThat(filter().filter(JSON, deep))
			.isEqualTo("{\"masked\":\"unreadable\"}")
			.satisfies(PayloadMaskingBodyFilterTest::isJson);
	}

	private static void isJson(final String body) {
		assertThatNoException().isThrownBy(() -> new ObjectMapper().readTree(body));
	}
}
