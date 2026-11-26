package se.sundsvall.supportmanagement.service.search.index;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.elasticsearch.client.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.model.ErrandIndex;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What is known about the state of the errand index beyond whether the cluster answers: whether it wants a full
 * reindex, and how close its mapping is to the number of fields it may hold.
 * <p>
 * Found out when the schema is put in place and after every rebuild, and kept here for the health of the service to
 * read, since reading the mapping of an index of thousands of fields every time the health is asked would be the
 * heaviest thing the health does.
 * <p>
 * An index wants a full reindex when its mapping could not be brought up to date, which is a change of a field the
 * mapping refuses, or when it is of another layout than the service writes ({@link ErrandIndex#LAYOUT_VERSION}). An
 * index without a layout of its own is taken to be of the first, which is what every index was before the layout was
 * kept.
 */
@Component
public class SearchIndexHealth {

	static final String LAYOUT_VERSION = "layoutVersion";
	static final String TOTAL_FIELDS_LIMIT = "index.mapping.total_fields.limit";
	static final int DEFAULT_TOTAL_FIELDS_LIMIT = 1000;
	static final int FIRST_LAYOUT = 1;
	static final String OTHER_LAYOUT = "The search index is of layout %d and the service writes layout %d. A full reindex (POST /search/reindex) rebuilds it";
	static final String NOT_UPDATED = "The mapping of the search index could not be brought up to date: %s. A full reindex (POST /search/reindex) rebuilds it";

	private static final Logger LOG = LoggerFactory.getLogger(SearchIndexHealth.class);
	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private final OpenSearchClient openSearch;

	private volatile String mappingNotUpdated;
	private volatile String otherLayout;
	private volatile FieldUsage fieldUsage;

	public SearchIndexHealth(final OpenSearchClient openSearch) {
		this.openSearch = openSearch;
	}

	/** Why the index wants a full reindex, empty while it does not. */
	public Optional<String> fullReindexWanted() {
		return Optional.ofNullable(mappingNotUpdated).or(() -> Optional.ofNullable(otherLayout));
	}

	/** How many fields the mapping of the index holds and may hold, empty until it has been read. */
	public Optional<FieldUsage> fieldUsage() {
		return Optional.ofNullable(fieldUsage);
	}

	/**
	 * Records that the mapping of the index could not be brought up to date by a cluster that answers, which is what a
	 * change of a field the mapping refuses looks like.
	 */
	public void mappingNotUpdated(final String reason) {
		mappingNotUpdated = NOT_UPDATED.formatted(reason);
		LOG.error(mappingNotUpdated);
	}

	/**
	 * Reads the mapping of the index: how many fields it holds, and which layout it is of. A failure to read it leaves
	 * what was known before, as the health reports the cluster that did not answer by itself.
	 */
	public void inspect() {
		try {
			final var mapping = firstIndexOf(get(new Request("GET", "/" + openSearch.errandWriteIndex() + "/_mapping"))).path("mappings");
			final var settingsRequest = new Request("GET", "/" + openSearch.errandWriteIndex() + "/_settings");
			settingsRequest.addParameter("flat_settings", "true");
			final var settings = firstIndexOf(get(settingsRequest)).path("settings");

			fieldUsage = new FieldUsage(countFields(mapping.path("properties")), settings.path(TOTAL_FIELDS_LIMIT).asInt(DEFAULT_TOTAL_FIELDS_LIMIT));

			final var layout = mapping.path("_meta").path(LAYOUT_VERSION).asInt(FIRST_LAYOUT);
			otherLayout = layout == ErrandIndex.LAYOUT_VERSION ? null : OTHER_LAYOUT.formatted(layout, ErrandIndex.LAYOUT_VERSION);
			if (otherLayout != null) {
				LOG.warn(otherLayout);
			}
		} catch (final IOException | RuntimeException e) {
			LOG.warn("The mapping of the search index could not be read: {}", e.getMessage());
		}
	}

	/**
	 * Records an index that was created anew, by a full reindex or because it was not there: it holds the current layout
	 * and nothing older, so whatever was wanted of it before is not.
	 */
	public void created() {
		try {
			final var request = new Request("PUT", "/" + openSearch.errandWriteIndex() + "/_mapping");
			request.setJsonEntity(MAPPER.writeValueAsString(Map.of("_meta", Map.of(LAYOUT_VERSION, ErrandIndex.LAYOUT_VERSION))));
			openSearch.restClient().performRequest(request);
			mappingNotUpdated = null;
		} catch (final IOException | RuntimeException e) {
			LOG.error("The layout of the search index could not be written to it, the health of the service will go on reporting it as of the first layout", e);
		}
		inspect();
	}

	private JsonNode get(final Request request) throws IOException {
		try (var content = openSearch.restClient().performRequest(request).getEntity().getContent()) {
			return MAPPER.readTree(content);
		}
	}

	/** The answer is keyed by the name of the index behind the alias asked for, which is the one the alias points at. */
	private static JsonNode firstIndexOf(final JsonNode response) {
		return response.isObject() && !response.isEmpty() ? response.iterator().next() : MAPPER.missingNode();
	}

	/**
	 * Counts fields the way the limit does: every field, every object and every sub field under {@code fields}.
	 */
	static int countFields(final JsonNode properties) {
		var count = 0;
		for (final var field : properties.properties()) {
			count++;
			final var definition = field.getValue();
			count += definition.path("fields").size();
			if (definition.has("properties")) {
				count += countFields(definition.path("properties"));
			}
		}
		return count;
	}

	/**
	 * @param count how many fields the mapping holds
	 * @param limit how many it may hold, beyond which a document bringing another is refused
	 */
	public record FieldUsage(int count, int limit) {

		/** How much of the limit is used, in percent. */
		public int percent() {
			return limit <= 0 ? 100 : (int) ((long) count * 100 / limit);
		}
	}
}
