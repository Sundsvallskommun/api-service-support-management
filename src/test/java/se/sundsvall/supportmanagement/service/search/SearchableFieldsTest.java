package se.sundsvall.supportmanagement.service.search;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which names a route may search. What matters most here is the last case: a name bound to nothing is refused, so a
 * shape of query nobody anticipated cannot open a field by going unrecognised.
 */
class SearchableFieldsTest {

	private static final Set<ProtectedResource> EVERY_RESOURCE = Stream.of(ProtectedResource.values()).collect(toSet());

	@Test
	void aRouteHeldToNothingSearchesEveryFieldThatIsBoundToSomething() {
		final var fields = SearchableFields.of(EVERY_RESOURCE, null);

		assertThat(fields.unrestricted()).isTrue();
		assertThat(fields.allows("title")).isTrue();
		assertThat(fields.allows("description")).isTrue();
		assertThat(fields.allows("communications.subject")).isTrue();
		assertThat(fields.allows("jsonParameters.any.path")).isTrue();
		assertThat(fields.allows("jsonParametersText")).isTrue();
	}

	@Test
	void whatARoleKeepsFromTheUserIsNotSearchable() {
		final var fields = SearchableFields.of(EVERY_RESOURCE, Map.of(ErrandField.TITLE, Set.of()));

		assertThat(fields.unrestricted()).isFalse();
		assertThat(fields.allows("title")).isTrue();
		assertThat(fields.refusal("description")).contains("Field 'description'");
		assertThat(fields.refusal("stakeholders.lastName")).contains("Field 'stakeholders'");
		// A resource carries fields no field of the errand names, and is governed by the resource grant alone
		assertThat(fields.allows("communications.subject")).isTrue();
	}

	@Test
	void whatTheLabelsDoNotReachIsNotSearchableEither() {
		final var fields = SearchableFields.of(Set.of(), null);

		assertThat(fields.unrestricted()).isFalse();
		assertThat(fields.refusal("communications.subject")).contains("Resource 'errand/communication'");
		assertThat(fields.refusal("decisions.title")).contains("Resource 'errand/decision'");
		assertThat(fields.allows("title")).isTrue();
	}

	/**
	 * Asking whether an object exists asks about everything under it, so the name of the object follows the resource it
	 * belongs to.
	 */
	@Test
	void theNameOfAnObjectFollowsTheResourceItBelongsTo() {
		assertThat(SearchableFields.of(EVERY_RESOURCE, null).allows("communications")).isTrue();
		assertThat(SearchableFields.of(Set.of(), null).refusal("communications")).contains("Resource 'errand/communication'");
	}

	@Test
	void aKeyedFieldGrantedKeysOpensThoseKeysWhereTheIndexTellsThemApart() {
		final var fields = SearchableFields.of(EVERY_RESOURCE, Map.of(ErrandField.JSON_PARAMETERS, Set.of("granted")));

		assertThat(fields.allows("jsonParameters.granted")).isTrue();
		assertThat(fields.allows("jsonParameters.granted.deep.path")).isTrue();
		assertThat(fields.refusal("jsonParameters.other.path")).contains("Key 'other' of Field 'jsonParameters'");
		// A key is matched whole: a longer name starting with the same letters is another key
		assertThat(fields.refusal("jsonParameters.grantedish.path")).contains("Key 'grantedish' of Field 'jsonParameters'");
		// The text of every key together, which single keys do not open
		assertThat(fields.refusal("jsonParametersText")).contains("Field 'jsonParameters' beyond its keys");
	}

	/**
	 * The object of a keyed field, named without a key under it, asks about every key at once - including the keys the
	 * route was not granted. It used to ask for a substring longer than the name and answer the client with a 500.
	 */
	@Test
	void theObjectOfAKeyedFieldAsksAboutEveryKeyAndIsRefused() {
		final var fields = SearchableFields.of(EVERY_RESOURCE, Map.of(ErrandField.JSON_PARAMETERS, Set.of("granted")));

		assertThat(fields.refusal("jsonParameters")).contains("Field 'jsonParameters' beyond its keys");
		assertThat(fields.refusal("jsonParameters.raw")).contains("Field 'jsonParameters' beyond its keys");
		assertThat(fields.allows("jsonParameters.granted.x")).isTrue();
	}

	@Test
	void aKeyedFieldWhoseKeysShareAnIndexFieldOpensNoneOfItByKey() {
		final var fields = SearchableFields.of(EVERY_RESOURCE, Map.of(ErrandField.PARAMETERS, Set.of("granted")));

		assertThat(fields.refusal("parameters.values")).contains("Field 'parameters' beyond its keys");
		assertThat(fields.refusal("parameters.key")).contains("Field 'parameters' beyond its keys");
	}

	@Test
	void theKeywordTwinFollowsTheFieldItDoubles() {
		final var fields = SearchableFields.of(EVERY_RESOURCE, Map.of(ErrandField.JSON_PARAMETERS, Set.of("granted")));

		assertThat(fields.allows("jsonParameters.granted.path.raw")).isTrue();
		assertThat(fields.refusal("jsonParameters.other.path.raw")).contains("Key 'other' of Field 'jsonParameters'");
	}

	@Test
	void aSortIsHeldToTheFieldTheOrderedPropertyBelongsTo() {
		final var fields = SearchableFields.of(EVERY_RESOURCE, Map.of(ErrandField.TITLE, Set.of()));

		assertThat(fields.wholeFieldRefusal(ErrandField.TITLE)).isEmpty();
		assertThat(fields.wholeFieldRefusal(ErrandField.CREATED)).contains("Field 'created'");
		// The properties category and type belong to the classification, which is what a sort on them says something about
		assertThat(fields.wholeFieldRefusal(ErrandField.CLASSIFICATION)).contains("Field 'classification'");
		assertThat(SearchableFields.of(EVERY_RESOURCE, null).wholeFieldRefusal(ErrandField.CREATED)).isEmpty();
	}

	@Test
	void theFieldsAWordIsLookedForInAreThoseTheRouteMaySearch() {
		final var fields = SearchableFields.of(EVERY_RESOURCE, Map.of(ErrandField.TITLE, Set.of()));

		assertThat(fields.openFields(List.of("title", "description", "communications.subject"))).containsExactly("title", "communications.subject");
	}

	/**
	 * The point of saying what is open rather than what is closed: a name nobody bound is refused, whether it is the
	 * bookkeeping of access control itself or a shape of query that was never thought of.
	 */
	@Test
	void aNameBoundToNothingIsRefusedOnEveryRoute() {
		assertThat(SearchableFields.of(EVERY_RESOURCE, null).refusal("accessLabels.metadataLabelId")).contains("'accessLabels.metadataLabelId'");
		assertThat(SearchableFields.of(EVERY_RESOURCE, null).refusal("-communications.subject")).contains("'-communications.subject'");
		assertThat(SearchableFields.of(EVERY_RESOURCE, null).refusal("u0063ommunications.subject")).contains("'u0063ommunications.subject'");
		assertThat(SearchableFields.of(EVERY_RESOURCE, null).refusal("")).contains("''");
	}
}
