package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandLabelServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@InjectMocks
	private ErrandLabelService service;

	@Test
	void expandLabelsToAncestorChain_leafExpandsToFullChain() {
		final var leafId = "leaf-id";
		final var parentId = "parent-id";
		final var childId = "child-id";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(leafId)));

		when(metadataLabelRepositoryMock.findAllById(Set.of(leafId)))
			.thenReturn(List.of(MetadataLabelEntity.create().withId(leafId).withResourcePath("parent/child/leaf")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child")))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(childId).withResourcePath("parent/child")));

		service.expandLabelsToAncestorChain(errandEntity);

		assertThat(errandEntity.getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(leafId, parentId, childId);

		verify(metadataLabelRepositoryMock).findAllById(Set.of(leafId));
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child"));
	}

	@Test
	void expandLabelsToAncestorChain_partialChainExpandsCorrectly() {
		final var leafId = "leaf-id";
		final var parentId = "parent-id";
		final var childId = "child-id";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of(
				ErrandLabelEmbeddable.create().withMetadataLabelId(parentId),
				ErrandLabelEmbeddable.create().withMetadataLabelId(leafId)));

		when(metadataLabelRepositoryMock.findAllById(Set.of(parentId, leafId)))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(leafId).withResourcePath("parent/child/leaf")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child")))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(childId).withResourcePath("parent/child")));

		service.expandLabelsToAncestorChain(errandEntity);

		assertThat(errandEntity.getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(parentId, leafId, childId);

		verify(metadataLabelRepositoryMock).findAllById(Set.of(parentId, leafId));
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child"));
	}

	@Test
	void expandLabelsToAncestorChain_emptyLabels_noRepoInteraction() {
		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of());

		service.expandLabelsToAncestorChain(errandEntity);

		assertThat(errandEntity.getLabels()).isEmpty();
		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void expandLabelsToAncestorChain_alreadyFullChain_noLabelsAdded() {
		final var leafId = "leaf-id";
		final var parentId = "parent-id";
		final var childId = "child-id";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of(
				ErrandLabelEmbeddable.create().withMetadataLabelId(parentId),
				ErrandLabelEmbeddable.create().withMetadataLabelId(childId),
				ErrandLabelEmbeddable.create().withMetadataLabelId(leafId)));

		when(metadataLabelRepositoryMock.findAllById(Set.of(parentId, childId, leafId)))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(childId).withResourcePath("parent/child"),
				MetadataLabelEntity.create().withId(leafId).withResourcePath("parent/child/leaf")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child")))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(childId).withResourcePath("parent/child")));

		service.expandLabelsToAncestorChain(errandEntity);

		assertThat(errandEntity.getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(parentId, childId, leafId);

		verify(metadataLabelRepositoryMock).findAllById(Set.of(parentId, childId, leafId));
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child"));
	}

	@Test
	void validateLabelVersions_noVersions_noRepoInteraction() {
		var labels = List.of(
			new ErrandLabel().withId("id-1"),
			new ErrandLabel().withId("id-2"));

		service.validateVersions(labels);

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void validateLabelVersions_nullLabels_noRepoInteraction() {
		service.validateVersions(null);

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void validateLabelVersions_versionsMatch_noException() {
		var labelId = "label-id-1";
		when(metadataLabelRepositoryMock.findAllById(List.of(labelId)))
			.thenReturn(List.of(MetadataLabelEntity.create().withId(labelId).withVersion(3L)));

		service.validateVersions(List.of(new ErrandLabel().withId(labelId).withVersion(3L)));

		verify(metadataLabelRepositoryMock).findAllById(List.of(labelId));
	}

	@Test
	void validateLabelVersions_versionMismatch_throws412() {
		var labelId = "label-id-1";
		when(metadataLabelRepositoryMock.findAllById(List.of(labelId)))
			.thenReturn(List.of(MetadataLabelEntity.create().withId(labelId).withVersion(5L)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.validateVersions(List.of(new ErrandLabel().withId(labelId).withVersion(3L))))
			.withMessageContaining(labelId)
			.withMessageContaining("3")
			.withMessageContaining("5");

		verify(metadataLabelRepositoryMock).findAllById(List.of(labelId));
	}

	@Test
	void validateLabelVersions_nullVersionInDb_noException() {
		var labelId = "label-id-1";
		when(metadataLabelRepositoryMock.findAllById(List.of(labelId)))
			.thenReturn(List.of(MetadataLabelEntity.create().withId(labelId)));

		service.validateVersions(List.of(new ErrandLabel().withId(labelId).withVersion(1L)));

		verify(metadataLabelRepositoryMock).findAllById(List.of(labelId));
	}
}
