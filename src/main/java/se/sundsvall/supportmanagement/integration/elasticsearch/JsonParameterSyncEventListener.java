package se.sundsvall.supportmanagement.integration.elasticsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

import static java.util.Optional.ofNullable;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

public class JsonParameterSyncEventListener {

	private static final Logger LOG = LoggerFactory.getLogger(JsonParameterSyncEventListener.class);
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	private final ErrandsRepository errandsRepository;
	private final JsonParameterElasticRepository elasticRepository;
	private final ObjectMapper objectMapper;

	public JsonParameterSyncEventListener(final ErrandsRepository errandsRepository,
		final JsonParameterElasticRepository elasticRepository,
		final ObjectMapper objectMapper) {
		this.errandsRepository = errandsRepository;
		this.elasticRepository = elasticRepository;
		this.objectMapper = objectMapper;
	}

	@Async
	@TransactionalEventListener(phase = AFTER_COMMIT)
	public void onJsonParameterSync(final JsonParameterSyncEvent event) {
		switch (event.type()) {
			case UPSERT -> syncErrandJsonParameters(event);
			case DELETE -> deleteErrandJsonParameters(event);
		}
	}

	private void syncErrandJsonParameters(final JsonParameterSyncEvent event) {
		try {
			final var errandOptional = errandsRepository.findById(event.errandId());
			if (errandOptional.isEmpty()) {
				LOG.warn("Errand {} not found for ES sync, skipping", event.errandId());
				return;
			}
			final var errand = errandOptional.get();

			// Delete existing documents for this errand before re-indexing
			elasticRepository.deleteByErrandId(event.errandId());

			final var jsonParameters = errand.getJsonParameters();
			if (jsonParameters == null || jsonParameters.isEmpty()) {
				return;
			}

			final var documents = jsonParameters.stream()
				.map(param -> JsonParameterDocument.create()
					.withId(param.getId())
					.withErrandId(event.errandId())
					.withNamespace(event.namespace())
					.withMunicipalityId(event.municipalityId())
					.withParameterKey(param.getKey())
					.withValue(parseJsonValue(param.getValue())))
				.toList();

			elasticRepository.saveAll(documents);
			LOG.debug("Synced {} JSON parameters for errand {} to Elasticsearch", documents.size(), event.errandId());
		} catch (final Exception e) {
			LOG.error("Failed to sync JSON parameters for errand {} to Elasticsearch", event.errandId(), e);
		}
	}

	private void deleteErrandJsonParameters(final JsonParameterSyncEvent event) {
		try {
			elasticRepository.deleteByErrandId(event.errandId());
			LOG.debug("Deleted JSON parameters for errand {} from Elasticsearch", event.errandId());
		} catch (final Exception e) {
			LOG.error("Failed to delete JSON parameters for errand {} from Elasticsearch", event.errandId(), e);
		}
	}

	private Map<String, Object> parseJsonValue(final String jsonString) {
		return ofNullable(jsonString)
			.map(json -> {
				try {
					return objectMapper.readValue(json, MAP_TYPE);
				} catch (final Exception e) {
					LOG.warn("Failed to parse JSON value for ES indexing: {}", e.getMessage());
					return Map.<String, Object>of("_raw", json);
				}
			})
			.orElse(Map.of());
	}
}
