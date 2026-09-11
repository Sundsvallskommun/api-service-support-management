package se.sundsvall.supportmanagement.integration.db.model;

import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class JsonParameterEntityTest {

	// The links to the handling artefacts are collections the parameter tears down but is not identified by, so none of
	// them takes part in equality or in the string form. Comparing them would also walk back into the artefacts, which
	// point at the errand this parameter already belongs to.
	private static final String[] COLLECTIONS = {
		"statementLinks", "investigationLinks", "investigationSectionLinks", "decisionLinks", "measureLinks"
	};

	private static String[] excluding(final String... properties) {
		return Stream.concat(Stream.of(properties), Stream.of(COLLECTIONS)).toArray(String[]::new);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(JsonParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(excluding("errandEntity", "version")),
			hasValidBeanEqualsExcluding(excluding("errandEntity", "version")),
			hasValidBeanToStringExcluding(excluding("errandEntity"))));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "id";
		final var key = "key";
		final var schemaId = "schemaId";
		final var value = "{\"field\": \"value\"}";
		final var errandEntity = ErrandEntity.create().withId("errandId");
		final var version = 1L;
		final var statementLinks = List.of(StatementJsonParameterEntity.create());
		final var investigationLinks = List.of(InvestigationJsonParameterEntity.create());
		final var investigationSectionLinks = List.of(InvestigationSectionJsonParameterEntity.create());
		final var decisionLinks = List.of(DecisionJsonParameterEntity.create());
		final var measureLinks = List.of(MeasureJsonParameterEntity.create());

		final var jsonParameterEntity = JsonParameterEntity.create()
			.withId(id)
			.withErrandEntity(errandEntity)
			.withKey(key)
			.withSchemaId(schemaId)
			.withValue(value)
			.withVersion(version)
			.withStatementLinks(statementLinks)
			.withInvestigationLinks(investigationLinks)
			.withInvestigationSectionLinks(investigationSectionLinks)
			.withDecisionLinks(decisionLinks)
			.withMeasureLinks(measureLinks);

		assertThat(jsonParameterEntity).hasNoNullFieldsOrProperties();
		assertThat(jsonParameterEntity.getId()).isEqualTo(id);
		assertThat(jsonParameterEntity.getKey()).isEqualTo(key);
		assertThat(jsonParameterEntity.getSchemaId()).isEqualTo(schemaId);
		assertThat(jsonParameterEntity.getValue()).isEqualTo(value);
		assertThat(jsonParameterEntity.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(jsonParameterEntity.getVersion()).isEqualTo(version);
		assertThat(jsonParameterEntity.getStatementLinks()).isEqualTo(statementLinks);
		assertThat(jsonParameterEntity.getInvestigationLinks()).isEqualTo(investigationLinks);
		assertThat(jsonParameterEntity.getInvestigationSectionLinks()).isEqualTo(investigationSectionLinks);
		assertThat(jsonParameterEntity.getDecisionLinks()).isEqualTo(decisionLinks);
		assertThat(jsonParameterEntity.getMeasureLinks()).isEqualTo(measureLinks);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(JsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new JsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
