package se.sundsvall.supportmanagement.integration.elasticsearch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.elasticsearch.JsonParameterSyncEvent.Type.DELETE;
import static se.sundsvall.supportmanagement.integration.elasticsearch.JsonParameterSyncEvent.Type.UPSERT;

class JsonParameterSyncEventTest {

	@Test
	void testUpsertFactory() {
		final var errandId = "errandId";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var event = JsonParameterSyncEvent.upsert(errandId, namespace, municipalityId);

		assertThat(event.errandId()).isEqualTo(errandId);
		assertThat(event.namespace()).isEqualTo(namespace);
		assertThat(event.municipalityId()).isEqualTo(municipalityId);
		assertThat(event.type()).isEqualTo(UPSERT);
	}

	@Test
	void testDeleteFactory() {
		final var errandId = "errandId";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var event = JsonParameterSyncEvent.delete(errandId, namespace, municipalityId);

		assertThat(event.errandId()).isEqualTo(errandId);
		assertThat(event.namespace()).isEqualTo(namespace);
		assertThat(event.municipalityId()).isEqualTo(municipalityId);
		assertThat(event.type()).isEqualTo(DELETE);
	}
}
