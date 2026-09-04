package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelMoveWorkerTest {

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@InjectMocks
	private LabelMoveWorker worker;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandCaptor;

	@Test
	void migrateErrandsForMovedLabel_delegatesToRebuildLabels() {
		var movedId = "moved-id";
		var errand = errandWithAccessLabels(movedId);

		when(errandsRepositoryMock.findAllByLabelsMetadataLabelId(movedId)).thenReturn(List.of(errand));
		when(metadataLabelRepositoryMock.findAllById(List.of(movedId)))
			.thenReturn(List.of(labelEntity(movedId, null)));
		when(errandServiceMock.persistLabelUpdate(any())).thenReturn(errand);

		worker.migrateErrandsForMovedLabel(movedId);

		verify(errandsRepositoryMock).findAllByLabelsMetadataLabelId(movedId);
		verify(metadataLabelRepositoryMock).findAllById(List.of(movedId));
		verify(errandServiceMock).persistLabelUpdate(errand);
	}

	@Test
	void rebuildLabels_leafOnlyInSubtree_fullChainReplaced() {
		// Errand has one leaf in the moved subtree. After move, leaf has two ancestors (grandparent, parent).
		var grandparentId = "gp";
		var parentId = "p";
		var leafId = "leaf";

		var grandparent = labelEntity(grandparentId, null);
		var parent = labelEntity(parentId, grandparent);
		var leaf = labelEntity(leafId, parent);

		var errand = errandWithAccessLabels(leafId);
		when(metadataLabelRepositoryMock.findAllById(List.of(leafId))).thenReturn(List.of(leaf));
		when(errandServiceMock.persistLabelUpdate(any())).thenReturn(errand);

		worker.rebuildLabels(errand);

		verify(errandServiceMock).persistLabelUpdate(errandCaptor.capture());
		assertThat(errandCaptor.getValue().getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(leafId, parentId, grandparentId);
	}

	@Test
	void rebuildLabels_leafOutsideSubtree_chainUnchanged() {
		// Errand has one leaf entirely outside the moved subtree — single root node.
		var rootId = "root";
		var root = labelEntity(rootId, null);

		var errand = errandWithAccessLabels(rootId);
		when(metadataLabelRepositoryMock.findAllById(List.of(rootId))).thenReturn(List.of(root));
		when(errandServiceMock.persistLabelUpdate(any())).thenReturn(errand);

		worker.rebuildLabels(errand);

		verify(errandServiceMock).persistLabelUpdate(errandCaptor.capture());
		assertThat(errandCaptor.getValue().getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactly(rootId);
	}

	@Test
	void rebuildLabels_leavesInsideAndOutsideSubtree_onlyAffectedChainUpdated() {
		// Errand has two leaves: one inside the moved subtree (now has a new parent after move),
		// one outside (stays at root level).
		var newParentId = "new-parent";
		var movedLeafId = "moved-leaf";
		var outsideLeafId = "outside";

		var newParent = labelEntity(newParentId, null);
		var movedLeaf = labelEntity(movedLeafId, newParent);
		var outsideLeaf = labelEntity(outsideLeafId, null);

		var errand = errandWithAccessLabels(movedLeafId, outsideLeafId);
		when(metadataLabelRepositoryMock.findAllById(List.of(movedLeafId, outsideLeafId)))
			.thenReturn(List.of(movedLeaf, outsideLeaf));
		when(errandServiceMock.persistLabelUpdate(any())).thenReturn(errand);

		worker.rebuildLabels(errand);

		verify(errandServiceMock).persistLabelUpdate(errandCaptor.capture());
		assertThat(errandCaptor.getValue().getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(movedLeafId, newParentId, outsideLeafId);
	}

	@Test
	void rebuildLabels_leafIsMovedNodeItself_chainRebuiltFromNewParent() {
		// The errand's access label IS the moved node itself (not a descendant).
		var newParentId = "new-parent";
		var movedId = "moved";

		var newParent = labelEntity(newParentId, null);
		var moved = labelEntity(movedId, newParent);

		var errand = errandWithAccessLabels(movedId);
		when(metadataLabelRepositoryMock.findAllById(List.of(movedId))).thenReturn(List.of(moved));
		when(errandServiceMock.persistLabelUpdate(any())).thenReturn(errand);

		worker.rebuildLabels(errand);

		verify(errandServiceMock).persistLabelUpdate(errandCaptor.capture());
		assertThat(errandCaptor.getValue().getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(movedId, newParentId);
	}

	@Test
	void rebuildLabels_moveToRoot_chainIsLeafOnly() {
		// After move to root (null parent), the leaf is now a root — chain contains only itself.
		var movedId = "moved";
		var moved = labelEntity(movedId, null);

		var errand = errandWithAccessLabels(movedId);
		when(metadataLabelRepositoryMock.findAllById(List.of(movedId))).thenReturn(List.of(moved));
		when(errandServiceMock.persistLabelUpdate(any())).thenReturn(errand);

		worker.rebuildLabels(errand);

		verify(errandServiceMock).persistLabelUpdate(errandCaptor.capture());
		assertThat(errandCaptor.getValue().getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactly(movedId);
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock);
	}

	private static ErrandEntity errandWithAccessLabels(final String... leafIds) {
		var accessLabels = java.util.Arrays.stream(leafIds)
			.map(id -> AccessLabelEmbeddable.create().withMetadataLabelId(id))
			.toList();
		return ErrandEntity.create().withAccessLabels(accessLabels);
	}

	private static MetadataLabelEntity labelEntity(final String id, final MetadataLabelEntity parent) {
		return MetadataLabelEntity.create().withId(id).withParent(parent);
	}
}
