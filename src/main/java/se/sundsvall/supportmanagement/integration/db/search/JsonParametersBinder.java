package se.sundsvall.supportmanagement.integration.db.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collection;
import java.util.Optional;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static se.sundsvall.supportmanagement.integration.db.search.SearchAnalysisConfigurer.TEXT;

/**
 * Indexes a collection of JSON parameters as fields of their own, one per scalar in the documents, so that a search can
 * reach into them by path.
 * <p>
 * A parameter with key {@code vehicle} and value {@code {"regNo": "ABC123", "owner": {"name": "Anna"}}} becomes the
 * fields {@code jsonParameters.vehicle.regNo}, {@code jsonParameters.vehicle.owner.name} and so on, relative to the
 * element the binder sits on: the errand itself, or a measure, decision, statement or investigation of it. What those
 * fields look like is not said here but in search-mapping.json, since the fields do not exist until a document holding
 * them is indexed: the parameters are written to the index as the JSON they are, and the index maps every scalar in
 * them as it comes, as analyzed text with a lowercased keyword twin under {@code .raw}, so that both "a word in the
 * value" and "exactly this value" can be asked. All scalars are also gathered in {@code jsonParametersText} next to the
 * object, which is what a search without a field reaches.
 */
public class JsonParametersBinder implements PropertyBinder {

	public static final String FIELD = ErrandIndex.JSON_PARAMETERS;
	public static final String TEXT_FIELD = ErrandIndex.JSON_PARAMETERS_TEXT;

	// A dynamic object: the index maps whatever is written under it, guided by the dynamic templates of the mapping file
	private static final String NATIVE_MAPPING = "{\"type\": \"object\", \"dynamic\": \"true\"}";

	@Override
	public void bind(final PropertyBindingContext context) {
		context.dependencies()
			.use("key")
			.use("value");

		final var schema = context.indexSchemaElement();

		final var jsonField = schema.field(FIELD, f -> f.extension(ElasticsearchExtension.get()).asNative().mapping(NATIVE_MAPPING))
			.toReference();
		final var textField = schema.field(TEXT_FIELD, f -> f.asString().analyzer(TEXT))
			.multiValued()
			.toReference();

		context.bridge(Collection.class, new Bridge(jsonField, textField));
	}

	/**
	 * Writes the parameters as one JSON object keyed by parameter key, and never fails: a parameter that is not JSON is
	 * logged and left out, so that a bad value can not keep the errand out of the index.
	 */
	static final class Bridge implements PropertyBridge<Collection> {

		private static final Logger LOG = LoggerFactory.getLogger(JsonParametersBinder.class);

		private final IndexFieldReference<JsonElement> jsonField;
		private final IndexFieldReference<String> textField;

		Bridge(final IndexFieldReference<JsonElement> jsonField, final IndexFieldReference<String> textField) {
			this.jsonField = jsonField;
			this.textField = textField;
		}

		@Override
		public void write(final DocumentElement target, final Collection bridgedElement, final PropertyBridgeWriteContext context) {
			if (bridgedElement == null || bridgedElement.isEmpty()) {
				return;
			}

			final var json = new JsonObject();
			for (final Object element : bridgedElement) {
				if (element instanceof final JsonParameterValue parameter) {
					parse(parameter).ifPresent(value -> {
						json.add(parameter.getKey(), value);
						writeText(target, value);
					});
				}
			}

			if (!json.isEmpty()) {
				target.addValue(jsonField, json);
			}
		}

		private static Optional<JsonElement> parse(final JsonParameterValue parameter) {
			if (parameter.getKey() == null || parameter.getValue() == null) {
				return Optional.empty();
			}
			try {
				return Optional.of(JsonParser.parseString(parameter.getValue()));
			} catch (final RuntimeException e) {
				LOG.warn("JSON parameter '{}' is left out of the search index since its value could not be read as JSON: {}", parameter.getKey(), e.getMessage());
				return Optional.empty();
			}
		}

		/** Every scalar of the value, whatever its depth, into the field a search without a field reaches. */
		private void writeText(final DocumentElement target, final JsonElement value) {
			if (value.isJsonObject()) {
				value.getAsJsonObject().asMap().values().forEach(child -> writeText(target, child));
			} else if (value.isJsonArray()) {
				value.getAsJsonArray().forEach(child -> writeText(target, child));
			} else if (value.isJsonPrimitive()) {
				target.addValue(textField, value.getAsString());
			}
		}
	}
}
