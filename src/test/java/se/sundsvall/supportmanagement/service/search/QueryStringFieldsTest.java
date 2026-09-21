package se.sundsvall.supportmanagement.service.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

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
}
