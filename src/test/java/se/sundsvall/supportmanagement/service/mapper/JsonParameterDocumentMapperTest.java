package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.JsonParameterDocumentMapper.hasJsonParameters;
import static se.sundsvall.supportmanagement.service.mapper.JsonParameterDocumentMapper.toJsonParameterDocument;

class JsonParameterDocumentMapperTest {

	private static final String ERRAND_ID = "errandId";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";

	@Test
	void toJsonParameterDocumentWithNestedObject() {
		final var entity = buildErrandEntity(JsonParameterEntity.create()
			.withKey("facility")
			.withSchemaId("2281_facility_1.0")
			.withValue("{\"facilityId\":\"FAC-0001\",\"address\":{\"street\":\"Storgatan 1\",\"city\":\"Sundsvall\"}}"));

		final var document = toJsonParameterDocument(entity);

		assertThat(document.getId()).isEqualTo(ERRAND_ID);
		assertThat(document.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(document.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(document.getJsonParameters()).containsOnlyKeys("facility");

		@SuppressWarnings("unchecked")
		final var entry = (Map<String, Object>) document.getJsonParameters().get("facility");
		assertThat(entry).containsEntry("schemaId", "2281_facility_1.0");

		@SuppressWarnings("unchecked")
		final var data = (Map<String, Object>) entry.get("data");
		assertThat(data).containsEntry("facilityId", "FAC-0001");

		@SuppressWarnings("unchecked")
		final var address = (Map<String, Object>) data.get("address");
		assertThat(address).containsEntry("street", "Storgatan 1").containsEntry("city", "Sundsvall");
	}

	@Test
	void toJsonParameterDocumentWithArrayValue() {
		final var entity = buildErrandEntity(JsonParameterEntity.create()
			.withKey("inspections")
			.withSchemaId("schema-1.0")
			.withValue("[{\"inspector\":\"Anna Svensson\"},{\"inspector\":\"Bo Nilsson\"}]"));

		final var document = toJsonParameterDocument(entity);

		@SuppressWarnings("unchecked")
		final var entry = (Map<String, Object>) document.getJsonParameters().get("inspections");
		assertThat(entry.get("data")).isInstanceOf(List.class);
		assertThat((List<?>) entry.get("data")).hasSize(2);
	}

	@Test
	void toJsonParameterDocumentWithScalarValue() {
		final var entity = buildErrandEntity(JsonParameterEntity.create()
			.withKey("counter")
			.withSchemaId("schema-1.0")
			.withValue("42"));

		final var document = toJsonParameterDocument(entity);

		@SuppressWarnings("unchecked")
		final var entry = (Map<String, Object>) document.getJsonParameters().get("counter");
		assertThat(entry).containsEntry("data", 42);
	}

	@Test
	void toJsonParameterDocumentWithInvalidJsonValue() {
		final var entity = buildErrandEntity(JsonParameterEntity.create()
			.withKey("broken")
			.withSchemaId("schema-1.0")
			.withValue("{not valid json"));

		final var document = toJsonParameterDocument(entity);

		@SuppressWarnings("unchecked")
		final var entry = (Map<String, Object>) document.getJsonParameters().get("broken");
		assertThat(entry).containsEntry("schemaId", "schema-1.0").doesNotContainKey("data");
	}

	@Test
	void toJsonParameterDocumentWithNullValue() {
		final var entity = buildErrandEntity(JsonParameterEntity.create()
			.withKey("empty")
			.withSchemaId("schema-1.0"));

		final var document = toJsonParameterDocument(entity);

		@SuppressWarnings("unchecked")
		final var entry = (Map<String, Object>) document.getJsonParameters().get("empty");
		assertThat(entry).containsEntry("schemaId", "schema-1.0").doesNotContainKey("data");
	}

	@Test
	void toJsonParameterDocumentWithoutJsonParameters() {
		final var entity = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID);

		final var document = toJsonParameterDocument(entity);

		assertThat(document.getId()).isEqualTo(ERRAND_ID);
		assertThat(document.getJsonParameters()).isEmpty();
	}

	@Test
	void hasJsonParametersVariants() {
		assertThat(hasJsonParameters(buildErrandEntity(JsonParameterEntity.create().withKey("key")))).isTrue();
		assertThat(hasJsonParameters(ErrandEntity.create().withJsonParameters(emptyList()))).isFalse();
		assertThat(hasJsonParameters(ErrandEntity.create())).isFalse();
	}

	private static ErrandEntity buildErrandEntity(final JsonParameterEntity... jsonParameters) {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withJsonParameters(List.of(jsonParameters));
	}
}
