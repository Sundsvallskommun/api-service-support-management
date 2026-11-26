package se.sundsvall.supportmanagement.service.search;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JsonParameterFieldsTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
		jsonParameters.vehicle.owner:anna                          | jsonParameters.vehicle.owner._v:anna
		jsonParameters.vehicle.owner.raw:anna                      | jsonParameters.vehicle.owner._v.raw:anna
		jsonParameters.vehicle.owner.name:ana~                     | jsonParameters.vehicle.owner.name._v:ana~
		measures.jsonParameters.order.supplier:ljus*               | measures.jsonParameters.order.supplier._v:ljus*
		jsonParameters.vehicle.owner:"bo karlsson"~2^3             | jsonParameters.vehicle.owner._v:"bo karlsson"~2^3
		jsonParameters.vehicle.year:[2020 TO 2024}                 | jsonParameters.vehicle.year._v:[2020 TO 2024}
		jsonParameters.vehicle.owner:(anna OR bo)                  | jsonParameters.vehicle.owner._v:(anna OR bo)
		-jsonParameters.vehicle.owner:anna AND status:new          | -jsonParameters.vehicle.owner._v:anna AND status:new
		vatten AND jsonParameters.a.b:x OR jsonParameters.c:y      | vatten AND jsonParameters.a.b._v:x OR jsonParameters.c._v:y
		jsonParameters.granted-json.visible:true                   | jsonParameters.granted-json.visible._v:true
		jsonParameters.\\*.regNo:abc123                            | jsonParameters.\\*.regNo._v:abc123
		jsonParameters.\\*.regNo.raw:xyz789                        | jsonParameters.\\*.regNo._v.raw:xyz789
		jsonParameters.vehicle.\\*.name:bergström                  | jsonParameters.vehicle.\\*.name._v:bergström
		""")
	void namesUnderTheJsonParametersAreLookedForInTheValueBelowThem(final String query, final String expected) {
		assertThat(JsonParameterFields.rewrite(query)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
		\\*.supplier:ljusbolaget                        | (\\*.supplier:ljusbolaget OR \\*.supplier._v:ljusbolaget)
		\\*.supplier.raw:ljusbolaget                    | (\\*.supplier.raw:ljusbolaget OR \\*.supplier._v.raw:ljusbolaget)
		+\\*.supplier:(ljus* OR "el ab")^2 AND vatten   | +(\\*.supplier:(ljus* OR "el ab")^2 OR \\*.supplier._v:(ljus* OR "el ab")^2) AND vatten
		measures.\\*.supplier:[a TO m]                  | (measures.\\*.supplier:[a TO m] OR measures.\\*.supplier._v:[a TO m])
		""")
	void aWildcardThatMayReachIntoTheJsonParametersAsksForBoth(final String query, final String expected) {
		assertThat(JsonParameterFields.rewrite(query)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"vattenläcka status:new",
		"title:faktura AND priority:high",
		"svenson~",
		"jsonParametersText:anna",
		"_exists_:jsonParameters.vehicle.owner",
		"jsonParameters.vehicle.\\*:anna",
		"jsonParameters.\\*:anna",
		"\\*:anna",
		"title:jsonParameters.vehicle.owner",
		"jsonParameters:anna",
		"title:\"jsonParameters.a:b\"",
		"jsonParameters.a.b:",
		"jsonParameters.a.b:\"unterminated"
	})
	void everythingElseIsLeftAsItIs(final String query) {
		assertThat(JsonParameterFields.rewrite(query)).isEqualTo(query);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void nothingIsLeftAsNothing(final String query) {
		assertThat(JsonParameterFields.rewrite(query)).isEqualTo(query);
	}
}
