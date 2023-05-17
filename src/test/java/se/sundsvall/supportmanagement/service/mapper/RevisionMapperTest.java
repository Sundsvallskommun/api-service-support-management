package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;

class RevisionMapperTest {

	private static final String ENTITY_UUID = randomUUID().toString();
	private static final String REVISION_UUID = randomUUID().toString();
	private static final String DESCRIPTION_VALUE = "descriptionValue";
	private static final Integer VERSION = 666;
	private static final String SNAPSHOT = "snapshot";
	private static final String ENTITY_TYPE = "entityType";
	private static final OffsetDateTime CREATED = OffsetDateTime.now();

	@Test
	void toRevisionEntity() {
		final var revision = RevisionMapper.toRevisionEntity(createErrandEntity(), 5);

		assertThat(revision).isNotNull();
		assertThat(revision.getCreated()).isNull();
		assertThat(revision.getEntityId()).isEqualTo(ENTITY_UUID);
		assertThat(revision.getEntityType()).isEqualTo(ErrandEntity.class.getSimpleName());
		assertThat(revision.getId()).isNull();
		assertThat(revision.getSerializedSnapshot()).isEqualToIgnoringNewLines("{\"id\":\"" + ENTITY_UUID + "\",\"description\":\"" + DESCRIPTION_VALUE + "\"}");
		assertThat(revision.getVersion()).isEqualTo(5);
	}

	@Test
	void toSerializedSnapshot() {
		final var serializedSnapshot = RevisionMapper.toSerializedSnapshot(createErrandEntity());
		assertThat(serializedSnapshot).isEqualToIgnoringNewLines("{\"id\":\"" + ENTITY_UUID + "\",\"description\":\"" + DESCRIPTION_VALUE + "\"}");
	}

	@Test
	void toSerializedSnapshotFromNull() {
		assertThat(RevisionMapper.toSerializedSnapshot(null)).isNull();
	}

	@Test
	void toRevision() {
		final var revision = RevisionMapper.toRevision(createRevisionEntity());

		assertThat(revision).isNotNull()
			.extracting(
				Revision::getCreated,
				Revision::getEntityId,
				Revision::getEntityType,
				Revision::getId,
				Revision::getVersion)
			.containsExactly(
				CREATED,
				ENTITY_UUID,
				ENTITY_TYPE,
				REVISION_UUID,
				VERSION);
	}

	@Test
	void toRevisionFromNull() {
		assertThat(RevisionMapper.toRevision(null)).isNull();
	}

	@Test
	void toRevisions() {
		final var revisions = RevisionMapper.toRevisions(List.of(createRevisionEntity()));

		assertThat(revisions).hasSize(1)
			.extracting(
				Revision::getCreated,
				Revision::getEntityId,
				Revision::getEntityType,
				Revision::getId,
				Revision::getVersion)
			.containsExactly(tuple(
				CREATED,
				ENTITY_UUID,
				ENTITY_TYPE,
				REVISION_UUID,
				VERSION));
	}

	@Test
	void toRevisionsFromNull() {
		assertThat(RevisionMapper.toRevisions(null)).isEmpty();
	}

	@Test
	void toRevisionsFromEmptyList() {
		assertThat(RevisionMapper.toRevisions(emptyList())).isEmpty();
	}

	@Test
	void toRevisionsListContainingNulls() {
		assertThat(RevisionMapper.toRevisions(singletonList(null))).isEmpty();
	}

	private RevisionEntity createRevisionEntity() {
		return RevisionEntity.create()
			.withCreated(CREATED)
			.withEntityId(ENTITY_UUID)
			.withEntityType(ENTITY_TYPE)
			.withId(REVISION_UUID)
			.withSerializedSnapshot(SNAPSHOT)
			.withVersion(VERSION);
	}

	private ErrandEntity createErrandEntity() {
		return ErrandEntity.create()
			.withId(ENTITY_UUID)
			.withDescription(DESCRIPTION_VALUE);
	}
}
