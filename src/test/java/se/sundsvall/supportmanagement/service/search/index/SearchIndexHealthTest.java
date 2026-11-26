package se.sundsvall.supportmanagement.service.search.index;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.model.ErrandIndex;
import tools.jackson.databind.json.JsonMapper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchIndexHealthTest {

	private static final String MAPPING = """
		{"errand-000001": {"mappings": {%s"properties": {
			"title": {"type": "text", "fields": {"raw": {"type": "keyword"}}},
			"stakeholders": {"properties": {"firstName": {"type": "text"}, "lastName": {"type": "text"}}},
			"namespace": {"type": "keyword"}
		}}}}""";
	private static final String SETTINGS = """
		{"errand-000001": {"settings": {"index.mapping.total_fields.limit": "10"}}}""";

	@Mock
	private OpenSearchClient openSearchMock;

	@Mock
	private RestClient restClientMock;

	private final List<Request> requests = new ArrayList<>();

	private void clusterAnswers(final String mapping, final String settings) throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		when(restClientMock.performRequest(any())).thenAnswer(invocation -> {
			final Request request = invocation.getArgument(0);
			requests.add(request);
			final var response = mock(Response.class);
			if ("GET".equals(request.getMethod())) {
				final var entity = mock(HttpEntity.class);
				when(response.getEntity()).thenReturn(entity);
				final var body = request.getEndpoint().endsWith("_mapping") ? mapping : settings;
				when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes(UTF_8)));
			}
			return response;
		});
	}

	@Test
	void nothingIsKnownBeforeTheIndexIsLookedAt() {
		final var health = new SearchIndexHealth(openSearchMock);

		assertThat(health.fieldUsage()).isEmpty();
		assertThat(health.fullReindexWanted()).isEmpty();
	}

	@Test
	void countsTheFieldsTheWayTheLimitDoes() throws IOException {
		// title, title.raw, stakeholders, stakeholders.firstName, stakeholders.lastName and namespace
		clusterAnswers(MAPPING.formatted(""), SETTINGS);
		final var health = new SearchIndexHealth(openSearchMock);

		health.inspect();

		assertThat(health.fieldUsage()).contains(new SearchIndexHealth.FieldUsage(6, 10));
		assertThat(health.fieldUsage().orElseThrow().percent()).isEqualTo(60);
		assertThat(health.fullReindexWanted()).isEmpty();
		assertThat(requests.getLast().getParameters()).containsEntry("flat_settings", "true");
	}

	@Test
	void theDefaultLimitIsTakenWhereTheIndexSetsNone() throws IOException {
		clusterAnswers(MAPPING.formatted(""), "{\"errand-000001\": {\"settings\": {}}}");
		final var health = new SearchIndexHealth(openSearchMock);

		health.inspect();

		assertThat(health.fieldUsage()).contains(new SearchIndexHealth.FieldUsage(6, SearchIndexHealth.DEFAULT_TOTAL_FIELDS_LIMIT));
	}

	@Test
	void anIndexWithoutALayoutIsOfTheFirst() throws IOException {
		clusterAnswers(MAPPING.formatted(""), SETTINGS);
		final var health = new SearchIndexHealth(openSearchMock);

		health.inspect();

		assertThat(ErrandIndex.LAYOUT_VERSION).isEqualTo(SearchIndexHealth.FIRST_LAYOUT);
		assertThat(health.fullReindexWanted()).isEmpty();
	}

	@Test
	void anIndexOfAnotherLayoutWantsAFullReindex() throws IOException {
		clusterAnswers(MAPPING.formatted("\"_meta\": {\"layoutVersion\": 0}, "), SETTINGS);
		final var health = new SearchIndexHealth(openSearchMock);

		health.inspect();

		assertThat(health.fullReindexWanted()).contains(SearchIndexHealth.OTHER_LAYOUT.formatted(0, ErrandIndex.LAYOUT_VERSION));
	}

	@Test
	void aMappingThatCouldNotBeUpdatedWantsAFullReindexUntilTheIndexIsCreatedAnew() throws IOException {
		clusterAnswers(MAPPING.formatted(""), SETTINGS);
		final var health = new SearchIndexHealth(openSearchMock);

		health.mappingNotUpdated("mapper [title] cannot be changed from type [text] to [keyword]");
		health.inspect();

		assertThat(health.fullReindexWanted()).contains(SearchIndexHealth.NOT_UPDATED.formatted("mapper [title] cannot be changed from type [text] to [keyword]"));

		health.created();

		assertThat(health.fullReindexWanted()).isEmpty();
		final var write = requests.stream().filter(request -> "PUT".equals(request.getMethod())).findFirst().orElseThrow();
		assertThat(write.getEndpoint()).isEqualTo("/errand-write/_mapping");
		assertThat(JsonMapper.builder().build().readTree(EntityUtils.toString(write.getEntity())))
			.isEqualTo(JsonMapper.builder().build().readTree("{\"_meta\": {\"layoutVersion\": " + ErrandIndex.LAYOUT_VERSION + "}}"));
	}

	@Test
	void aClusterThatCannotBeReadLeavesWhatWasKnown() throws IOException {
		clusterAnswers(MAPPING.formatted(""), SETTINGS);
		final var health = new SearchIndexHealth(openSearchMock);
		health.inspect();

		doThrow(new IOException("gone")).when(restClientMock).performRequest(any());
		health.inspect();

		assertThat(health.fieldUsage()).contains(new SearchIndexHealth.FieldUsage(6, 10));
	}
}
