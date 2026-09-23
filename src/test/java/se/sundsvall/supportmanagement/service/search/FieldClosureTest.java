package se.sundsvall.supportmanagement.service.search;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static org.assertj.core.api.Assertions.assertThat;

class FieldClosureTest {

	private static Set<ProtectedResource> reaching(final ProtectedResource... resources) {
		return Set.of(resources);
	}

	@Test
	void nothingClosedWhenEverythingIsReachedAndNothingRestricts() {
		final var closure = FieldClosure.of(reaching(ProtectedResource.values()), null);

		assertThat(closure.isOpen()).isTrue();
		assertThat(closure.refusal("communications.subject")).isEmpty();
		assertThat(closure.openFields(List.of("title", "decisions.title"))).containsExactly("title", "decisions.title");
	}

	@Test
	void resourcesNotReachedAreClosedByTheStartOfTheirNames() {
		final var closure = FieldClosure.of(reaching(ProtectedResource.DECISION), null);

		assertThat(closure.refusal("communications.subject")).contains("Resource 'errand/communication'");
		assertThat(closure.refusal("communications.messageBody.raw")).contains("Resource 'errand/communication'");
		assertThat(closure.refusal("decisions.title")).isEmpty();
		assertThat(closure.refusal("title")).isEmpty();
	}

	@Test
	void fieldsAndKeysTheRolesKeepFromTheUserAreClosed() {
		final var closure = FieldClosure.of(reaching(ProtectedResource.values()), Map.of(
			ErrandField.TITLE, Set.of(),
			ErrandField.PARAMETERS, Set.of("granted-key"),
			ErrandField.JSON_PARAMETERS, Set.of("granted-json")));

		// A field of a name, or one under it, but not a field whose name merely starts the same way
		assertThat(closure.refusal("title")).isEmpty();
		assertThat(closure.refusal("title.raw")).isEmpty();
		assertThat(closure.refusal("description")).contains("Field 'description'");
		assertThat(closure.refusal("stakeholders.lastName")).contains("Field 'stakeholders'");
		assertThat(closure.refusal("category")).contains("Field 'classification'");
		// A JSON key of its own is open, another is closed, and the text of all of them is closed
		assertThat(closure.refusal("jsonParameters.granted-json.visible")).isEmpty();
		assertThat(closure.refusal("jsonParameters.hidden-json.secret")).contains("Key 'hidden-json' of Field 'jsonParameters'");
		assertThat(closure.refusal("jsonParametersText")).contains("Field 'jsonParameters' beyond its keys");
		// Parameter values are shared by every key
		assertThat(closure.refusal("parameters.values")).contains("Field 'parameters' beyond its keys");
		// Resources are not fields of the errand and stay open
		assertThat(closure.refusal("communications.subject")).isEmpty();
		assertThat(closure.openFields(List.of("title", "description", "communications.subject"))).containsExactly("title", "communications.subject");
	}
}
