package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static org.assertj.core.api.Assertions.assertThat;

class LabelTreeSnapshotTest {

	@Test
	void of_indexesExistingLabelsByPath() {
		var snapshot = LabelTreeSnapshot.of(List.of(
			labelEntity("root-id", "CATEGORY", "ROOT"),
			labelEntity("child-id", "CATEGORY/TYPE", "ROOT")));

		assertThat(snapshot.exists("CATEGORY")).isTrue();
		assertThat(snapshot.exists("CATEGORY/TYPE")).isTrue();
		assertThat(snapshot.exists("CATEGORY/MISSING")).isFalse();
		assertThat(snapshot.exists(LabelTreeSnapshot.ROOT)).isTrue();
		assertThat(snapshot.idAt("CATEGORY")).isEqualTo("root-id");
		assertThat(snapshot.isLeaf("CATEGORY")).isFalse();
		assertThat(snapshot.isLeaf("CATEGORY/TYPE")).isTrue();
	}

	@Test
	void recordAdd_makesPathResolvableWithNullId() {
		var snapshot = LabelTreeSnapshot.of(List.of(labelEntity("root-id", "CATEGORY", "ROOT")));

		snapshot.recordAdd("CATEGORY/NEW_TYPE", null);

		assertThat(snapshot.exists("CATEGORY/NEW_TYPE")).isTrue();
		assertThat(snapshot.idAt("CATEGORY/NEW_TYPE")).isNull();
		assertThat(snapshot.isLeaf("CATEGORY")).isFalse();
		assertThat(snapshot.isLeaf("CATEGORY/NEW_TYPE")).isTrue();
	}

	@Test
	void recordDelete_removesPathAndClearsParentChild() {
		var snapshot = LabelTreeSnapshot.of(List.of(
			labelEntity("root-id", "CATEGORY", "ROOT"),
			labelEntity("child-id", "CATEGORY/TYPE", "ROOT")));

		snapshot.recordDelete("CATEGORY/TYPE");

		assertThat(snapshot.exists("CATEGORY/TYPE")).isFalse();
		assertThat(snapshot.isLeaf("CATEGORY")).isTrue();
	}

	@Test
	void recordMove_rebasesTheMovedNodeAndItsDescendants() {
		var snapshot = LabelTreeSnapshot.of(List.of(
			labelEntity("source-id", "SOURCE", "ROOT"),
			labelEntity("child-id", "SOURCE/CHILD", "ROOT"),
			labelEntity("dest-id", "DEST", "ROOT")));

		snapshot.recordMove("SOURCE", "DEST/SOURCE");

		assertThat(snapshot.exists("SOURCE")).isFalse();
		assertThat(snapshot.exists("SOURCE/CHILD")).isFalse();
		assertThat(snapshot.exists("DEST/SOURCE")).isTrue();
		assertThat(snapshot.idAt("DEST/SOURCE")).isEqualTo("source-id");
		assertThat(snapshot.exists("DEST/SOURCE/CHILD")).isTrue();
		assertThat(snapshot.idAt("DEST/SOURCE/CHILD")).isEqualTo("child-id");
		assertThat(snapshot.isLeaf("DEST")).isFalse();
		assertThat(snapshot.isLeaf("DEST/SOURCE")).isFalse();
		assertThat(snapshot.isLeaf("DEST/SOURCE/CHILD")).isTrue();
	}

	@Test
	void realIdsAtOrUnder_collectsOnlyPersistedIdsInTheSubtree() {
		var snapshot = LabelTreeSnapshot.of(List.of(
			labelEntity("root-id", "ROOT", "ROOT"),
			labelEntity("child-id", "ROOT/CHILD", "ROOT")));
		snapshot.recordAdd("ROOT/CHILD/GRANDCHILD", null);

		var ids = snapshot.realIdsAtOrUnder("ROOT");

		assertThat(ids).containsExactlyInAnyOrder("root-id", "child-id");
	}

	@Test
	void recordMerge_removesEverySourcePath() {
		var snapshot = LabelTreeSnapshot.of(List.of(
			labelEntity("target-id", "TARGET", "ROOT"),
			labelEntity("source-1-id", "SOURCE_1", "ROOT"),
			labelEntity("source-2-id", "SOURCE_2", "ROOT")));

		snapshot.recordMerge("TARGET", List.of("SOURCE_1", "SOURCE_2"));

		assertThat(snapshot.exists("TARGET")).isTrue();
		assertThat(snapshot.exists("SOURCE_1")).isFalse();
		assertThat(snapshot.exists("SOURCE_2")).isFalse();
	}

	private static MetadataLabelEntity labelEntity(final String id, final String resourcePath, final String classification) {
		final var resourceName = resourcePath.contains("/") ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1) : resourcePath;
		return MetadataLabelEntity.create().withId(id).withResourceName(resourceName).withResourcePath(resourcePath).withClassification(classification);
	}
}
