package se.sundsvall.supportmanagement.config.masking;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
	void keepsEveryPropertyNameAndEveryBooleanAndNull() {
		final var result = filter("$..fileSize").filter(JSON,
			"""
				{"fileSize":274,"internal":false,"viewed":null,"fileName":"anna.pdf"}""");

		assertThat(result).isEqualTo(
			"""
				{"fileSize":274,"internal":false,"viewed":null,"fileName":"[masked]"}""");
	}

	@Test
	void judgesANumberByTheKeepListJustAsItJudgesText() {
		final var result = filter("$..fileSize").filter(JSON,
			"""
				{"fileSize":274,"legalId":9001011234,"counted":17}""");

		assertThat(result).isEqualTo(
			"""
				{"fileSize":274,"legalId":"[masked]","counted":"[masked]"}""");
	}

	@Test
	void withholdsTheCoordinatesOfAnAddressTheyWouldPinpoint() {
		// What the Citizen service answers with: the address is text, the coordinates are numbers, and masking the one
		// without the other would leave the registered address of a person in the log
		final var result = filter("$..id", "$..addressType").filter(JSON,
			"""
				{"id":"1","addresses":[{"addressType":"POPULATION_REGISTRATION_ADDRESS","address":"Storgatan 1",\
				"postalCode":"85230","city":"Sundsvall","xCoordLocal":6921345.12,"yCoordLocal":615234.56}]}""");

		assertThat(result)
			.doesNotContain("6921345", "615234", "Storgatan")
			.isEqualTo(
				"""
					{"id":"1","addresses":[{"addressType":"POPULATION_REGISTRATION_ADDRESS","address":"[masked]",\
					"postalCode":"[masked]","city":"[masked]","xCoordLocal":"[masked]","yCoordLocal":"[masked]"}]}""");
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
		final var result = filter("$..id", "$..totalElements").filter(JSON,
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
		assertThat(filter().filter("text/json", "{\"title\":\"Something broken\"}")).isEqualTo("{\"title\":\"Something broken\"}");
		assertThat(filter().filter(null, "{\"title\":\"Something broken\"}")).isEqualTo("{\"title\":\"Something broken\"}");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"application/json", "application/json;charset=UTF-8", "application/problem+json",
		"APPLICATION/JSON", "application/json ; charset=UTF-8",
		// A content type Logbook logs as JSON and MediaType.parseMediaType refuses to read. Judged by the text of the
		// header, as Logbook judges it, so that a client sending nonsense after the semicolon cannot have its payload
		// logged unmasked
		"application/json;charset=bogus", "application/json;boundary=\""
	})
	void masksEveryContentTypeLogbookLogsAsJson(final String contentType) {
		assertThat(filter("$..id").filter(contentType,
			"""
				{"id":"1","title":"Something broken"}"""))
			.isEqualTo(
				"""
					{"id":"1","title":"[masked]"}""");
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
		final var result = filter("$.*.id").filter(JSON,
			"""
				{
				  "errand": {
				    "id": "1",
				    "title": "Something broken"
				  },
				  "notification": {
				    "id": "2"
				  },
				  "revision": {
				    "previous": {
				      "id": "3"
				    }
				  }
				}""");

		// One segment is one field, so `errand` and `notification` are reached and `revision.previous` is one too deep
		assertThat(result).isEqualTo(
			"""
				{"errand":{"id":"1","title":"[masked]"},"notification":{"id":"2"},"revision":{"previous":{"id":"[masked]"}}}""");
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
