package se.sundsvall.supportmanagement.service.mapper;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

class RevisionMapperTest {

	private static final String UUID = randomUUID().toString();
	private static final String DESCRIPTION_VALUE = "descriptionValue";

	@Test
	void toRevisionEntity() {
		final var revision = RevisionMapper.toRevisionEntity(createEntity(), 5);

		assertThat(revision).isNotNull();
		assertThat(revision.getCreated()).isNull();
		assertThat(revision.getEntityId()).isEqualTo(UUID);
		assertThat(revision.getEntityType()).isEqualTo(ErrandEntity.class.getSimpleName());
		assertThat(revision.getId()).isNull();
		assertThat(revision.getSerializedSnapshot()).isEqualToIgnoringNewLines("{\"id\":\"" + UUID + "\",\"description\":\"" + DESCRIPTION_VALUE + "\"}");
		assertThat(revision.getVersion()).isEqualTo(5);
	}

	@Test
	void toSerializedSnapshot() {
		final var serializedSnapshot = RevisionMapper.toSerializedSnapshot(createEntity());
		assertThat(serializedSnapshot).isEqualToIgnoringNewLines("{\"id\":\"" + UUID + "\",\"description\":\"" + DESCRIPTION_VALUE + "\"}");
	}

	private ErrandEntity createEntity() {
		return ErrandEntity.create()
			.withId(UUID)
			.withDescription(DESCRIPTION_VALUE);
	}
}
