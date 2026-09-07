package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelMoveWorkerTest {

	private static final String JOB_ID = randomUUID().toString();
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String STARTED_BY = "joe01doe";
	private static final int BATCH_SIZE = 2;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private EventService eventServiceMock;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandCaptor;

	private LabelMoveWorker worker;

	private LabelMoveWorker worker() {
		if (worker == null) {
			worker = new LabelMoveWorker(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock,
				new LabelMoveProperties(BATCH_SIZE, 2));
		}
		return worker;
	}

	@Test
	void migrateErrandsForMovedLabel_delegatesToRebuildLabels() {
		var movedId = "moved-id";
		var errand = errandWithAccessLabels(movedId);

		when(errandsRepositoryMock.findAllByLabelsMetadataLabelId(movedId)).thenReturn(List.of(errand));
		when(metadataLabelRepositoryMock.findAllById(List.of(movedId)))
			.thenReturn(List.of(labelEntity(movedId, null)));
		when(errandServiceMock.persistLabelUpdate(any())).thenReturn(errand);

		worker().migrateErrandsForMovedLabel(movedId);

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

		worker().rebuildLabels(errand);

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

		worker().rebuildLabels(errand);

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

		worker().rebuildLabels(errand);

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

		worker().rebuildLabels(errand);

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

		worker().rebuildLabels(errand);

		verify(errandServiceMock).persistLabelUpdate(errandCaptor.capture());
		assertThat(errandCaptor.getValue().getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactly(movedId);
	}

	@Test
	void run_happyPath_reparentsSubtreeRestowsErrandsAndCompletesJob() {
		var movedId = "moved";
		var newParentId = "new-parent";
		var childId = "child";

		var newParent = labelEntity(newParentId, null, "TARGET");
		var moved = labelEntity(movedId, labelEntity("old-parent", null, "ROOT"), "ROOT/MOVED");
		var child = labelEntity(childId, moved, "ROOT/MOVED/CHILD");
		var errand = errandWithAccessLabels(childId);
		var pageable = PageRequest.of(0, BATCH_SIZE, Sort.by("id"));

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(metadataLabelRepositoryMock.findById(newParentId)).thenReturn(Optional.of(newParent));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "ROOT/MOVED/"))
			.thenReturn(List.of(child));
		when(errandsRepositoryMock.findByLabelsMetadataLabelId(movedId, pageable))
			.thenReturn(new PageImpl<>(List.of(errand), pageable, 1));

		worker().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, newParentId, STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).findById(newParentId);
		assertThat(moved.getParent()).isSameAs(newParent);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "ROOT/MOVED/");
		verify(metadataLabelRepositoryMock).saveAll(List.of(child));
		verify(metadataLabelRepositoryMock).flush();
		verify(errandsRepositoryMock).findByLabelsMetadataLabelId(movedId, pageable);
		// The label rebuild itself is ErrandService's job (persistLabelMigrationBatch), not the worker's - it hands
		// over the page exactly as read, still carrying its pre-move labels.
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	void run_moveToRoot_setsParentNullAndSkipsNewParentLookup() {
		var movedId = "moved";
		var moved = labelEntity(movedId, labelEntity("old-parent", null, "ROOT"), "ROOT/MOVED");
		var pageable = PageRequest.of(0, BATCH_SIZE, Sort.by("id"));

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "ROOT/MOVED/"))
			.thenReturn(List.of());
		when(errandsRepositoryMock.findByLabelsMetadataLabelId(movedId, pageable))
			.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		worker().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, STARTED_BY));

		assertThat(moved.getParent()).isNull();
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "ROOT/MOVED/");
		verify(metadataLabelRepositoryMock).saveAll(List.of());
		verify(metadataLabelRepositoryMock).flush();
		verify(errandsRepositoryMock).findByLabelsMetadataLabelId(movedId, pageable);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	void run_multiplePages_persistsEachPageInItsOwnBatchAndReportsProgressPerPage() {
		var movedId = "moved";
		var moved = labelEntity(movedId, null, "ROOT");
		var errand1 = errandWithAccessLabels(movedId).withId("errand-1");
		var errand2 = errandWithAccessLabels(movedId).withId("errand-2");
		var firstPage = PageRequest.of(0, 1, Sort.by("id"));
		var secondPage = PageRequest.of(1, 1, Sort.by("id"));
		var pagedWorker = new LabelMoveWorker(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock,
			new LabelMoveProperties(1, 2));

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "ROOT/"))
			.thenReturn(List.of());
		when(errandsRepositoryMock.findByLabelsMetadataLabelId(movedId, firstPage))
			.thenReturn(new PageImpl<>(List.of(errand1), firstPage, 2));
		when(errandsRepositoryMock.findByLabelsMetadataLabelId(movedId, secondPage))
			.thenReturn(new PageImpl<>(List.of(errand2), secondPage, 2));

		pagedWorker.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, STARTED_BY));

		verify(errandsRepositoryMock).findByLabelsMetadataLabelId(movedId, firstPage);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelId(movedId, secondPage);
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand1));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand2));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "ROOT/");
		verify(metadataLabelRepositoryMock).saveAll(List.of());
		verify(metadataLabelRepositoryMock).flush();
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	void run_labelNoLongerExists_failsJobWithoutTouchingErrandsOrAuditing() {
		var movedId = "gone";
		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.empty());

		worker().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.contains("no longer exists")));
		verifyNoInteractions(errandServiceMock, eventServiceMock);
		verify(errandsRepositoryMock, never()).findByLabelsMetadataLabelId(any(), any());
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock);
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

	private static MetadataLabelEntity labelEntity(final String id, final MetadataLabelEntity parent, final String resourcePath) {
		return MetadataLabelEntity.create().withId(id).withParent(parent).withResourcePath(resourcePath);
	}
}
