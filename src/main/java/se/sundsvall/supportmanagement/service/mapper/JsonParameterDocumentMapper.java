package se.sundsvall.supportmanagement.service.mapper;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.elasticsearch.model.JsonParameterDocument;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

public final class JsonParameterDocumentMapper {

	private static final Logger LOG = LoggerFactory.getLogger(JsonParameterDocumentMapper.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String SCHEMA_ID = "schemaId";
	private static final String DATA = "data";

	private JsonParameterDocumentMapper() {}

	/**
	 * Maps an errand entity to its Elasticsearch document. Each json parameter is flattened to
	 * {@code jsonParameters.{key}.schemaId} and {@code jsonParameters.{key}.data.{...}} where the longtext value is parsed
	 * as a JSON tree, dynamically mapped by Elasticsearch. An unparseable value is indexed with schemaId only.
	 *
	 * @param  entity the errand entity to map
	 * @return        document with flattened json parameters
	 */
	public static JsonParameterDocument toJsonParameterDocument(final ErrandEntity entity) {
		final var jsonParameters = new HashMap<String, Object>();
		ofNullable(entity.getJsonParameters()).orElse(emptyList())
			.forEach(jsonParameter -> jsonParameters.put(jsonParameter.getKey(), toParameterEntry(jsonParameter)));

		return JsonParameterDocument.create()
			.withId(entity.getId())
			.withNamespace(entity.getNamespace())
			.withMunicipalityId(entity.getMunicipalityId())
			.withJsonParameters(jsonParameters);
	}

	public static boolean hasJsonParameters(final ErrandEntity entity) {
		return !ofNullable(entity.getJsonParameters()).orElse(emptyList()).isEmpty();
	}

	private static Map<String, Object> toParameterEntry(final JsonParameterEntity jsonParameter) {
		final var entry = new HashMap<String, Object>();
		entry.put(SCHEMA_ID, jsonParameter.getSchemaId());
		ofNullable(toDataTree(jsonParameter.getValue())).ifPresent(data -> entry.put(DATA, data));
		return entry;
	}

	private static Object toDataTree(final String value) {
		if (isNull(value)) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(value, Object.class);
		} catch (final JacksonException e) {
			LOG.debug("Failed to parse json parameter value as JSON, indexing schemaId only: {}", e.getMessage());
			return null;
		}
	}
}
