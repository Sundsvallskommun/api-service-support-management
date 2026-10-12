package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

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
	@DisplayName("Verification that every label, the ancestors added included, is given the metadata it points at, so the errand answers with whole labels in the response to the write that set them")
	void settleAccessLabels_attachesTheMetadataOfEveryLabel() {
		final var leafId = "leaf-id";
		final var parentId = "parent-id";
		final var leaf = MetadataLabelEntity.create().withId(leafId).withResourcePath("parent/leaf");
		final var parent = MetadataLabelEntity.create().withId(parentId).withResourcePath("parent");
		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(leafId)));

		when(metadataLabelRepositoryMock.findAllById(Set.of(leafId))).thenReturn(List.of(leaf));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent"))).thenReturn(List.of(parent));
		when(metadataLabelRepositoryMock.findAllById(Set.of(leafId, parentId))).thenReturn(List.of(leaf, parent));

		service.settleAccessLabels(errandEntity);

		assertThat(errandEntity.getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId, ErrandLabelEmbeddable::getMetadataLabel)
			.containsExactlyInAnyOrder(tuple(leafId, leaf), tuple(parentId, parent));
		assertThat(errandEntity.getAccessLabels())
			.extracting(AccessLabelEmbeddable::getMetadataLabelId)
			.containsExactly(leafId);
	}

	@Test
	@DisplayName("Verification that a label read from the database keeps the metadata Hibernate gave it")
	void settleAccessLabels_leavesALoadedLabelAsItIs() {
		final var labelId = "label-id";
		final var loaded = MetadataLabelEntity.create().withId(labelId).withResourcePath("label");
		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(labelId).withMetadataLabel(loaded)));

		when(metadataLabelRepositoryMock.findAllById(Set.of(labelId))).thenReturn(List.of(MetadataLabelEntity.create().withId(labelId).withResourcePath("label")));

		service.settleAccessLabels(errandEntity);

		assertThat(errandEntity.getLabels()).singleElement().extracting(ErrandLabelEmbeddable::getMetadataLabel).isSameAs(loaded);
		assertThat(errandEntity.getAccessLabels()).extracting(AccessLabelEmbeddable::getMetadataLabelId).containsExactly(labelId);
	}

	@Test
	void settleAccessLabels_noLabels_noRepoInteraction() {
		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of());

		service.settleAccessLabels(errandEntity);

		assertThat(errandEntity.getAccessLabels()).isEmpty();
		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void validateLabels_nullLabels_noRepoInteraction() {
		assertThatNoException().isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, null));
		assertThatNoException().isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of()));

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void validateLabels_labelsOfTheNamespace_noException() {
		when(metadataLabelRepositoryMock.findAllById(Set.of("id-1", "id-2"))).thenReturn(List.of(label("id-1", NAMESPACE, MUNICIPALITY_ID), label("id-2", NAMESPACE, MUNICIPALITY_ID)));

		assertThatNoException().isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of(new ErrandLabel().withId("id-1"), new ErrandLabel().withId("id-2"))));
	}

	@Test
	@DisplayName("Verification that the namespace of a label is matched regardless of case, as the database matches it")
	void validateLabels_namespaceInAnotherCase_noException() {
		when(metadataLabelRepositoryMock.findAllById(Set.of("id-1"))).thenReturn(List.of(label("id-1", NAMESPACE.toUpperCase(), MUNICIPALITY_ID)));

		assertThatNoException().isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of(new ErrandLabel().withId("id-1"))));
	}

	@Test
	@DisplayName("Verification that an id spelled in another case than the label spells it is refused, since the errand would store it as sent")
	void validateLabels_idInAnotherCase_throws400() {
		when(metadataLabelRepositoryMock.findAllById(Set.of("ID-1"))).thenReturn(List.of(label("id-1", NAMESPACE, MUNICIPALITY_ID)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of(new ErrandLabel().withId("ID-1"))))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo("Label with id 'ID-1' does not exist in namespace 'namespace' for municipality 'municipalityId'");
			});
	}

	@Test
	@DisplayName("Verification that a label without an id is refused before anything is looked up, rather than left to fail the insert")
	void validateLabels_labelWithoutId_throws400() {
		final var labels = new ArrayList<ErrandLabel>();
		labels.add(new ErrandLabel().withId("id-1"));
		labels.add(new ErrandLabel());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, labels))
			.satisfies(problem -> assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST));

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void validateLabels_nullLabel_throws400() {
		final var labels = new ArrayList<ErrandLabel>();
		labels.add(null);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, labels))
			.satisfies(problem -> assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST));
	}

	@Test
	void validateLabels_unknownLabel_throws400() {
		when(metadataLabelRepositoryMock.findAllById(Set.of("unknown"))).thenReturn(List.of());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of(new ErrandLabel().withId("unknown"))))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo("Label with id 'unknown' does not exist in namespace 'namespace' for municipality 'municipalityId'");
			});
	}

	@ParameterizedTest
	@MethodSource("labelsOfOtherNamespaces")
	@DisplayName("Verification that a label of another namespace or municipality is refused in the same words as one that does not exist, so that the answer says nothing about other namespaces")
	void validateLabels_labelOfAnotherNamespace_throws400(final String namespace, final String municipalityId) {
		when(metadataLabelRepositoryMock.findAllById(Set.of("foreign"))).thenReturn(List.of(label("foreign", namespace, municipalityId)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of(new ErrandLabel().withId("foreign"))))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo("Label with id 'foreign' does not exist in namespace 'namespace' for municipality 'municipalityId'");
			});
	}

	@Test
	void validateLabels_versionsMatch_noException() {
		when(metadataLabelRepositoryMock.findAllById(Set.of("label-id-1"))).thenReturn(List.of(label("label-id-1", NAMESPACE, MUNICIPALITY_ID).withVersion(3L)));

		assertThatNoException().isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of(new ErrandLabel().withId("label-id-1").withVersion(3L))));
	}

	@Test
	void validateLabels_versionMismatch_throws412() {
		final var labelId = "label-id-1";
		when(metadataLabelRepositoryMock.findAllById(Set.of(labelId))).thenReturn(List.of(label(labelId, NAMESPACE, MUNICIPALITY_ID).withVersion(5L)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of(new ErrandLabel().withId(labelId).withVersion(3L))))
			.satisfies(problem -> assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED))
			.withMessageContaining(labelId)
			.withMessageContaining("3")
			.withMessageContaining("5");
	}

	@Test
	void validateLabels_nullVersionInDb_noException() {
		when(metadataLabelRepositoryMock.findAllById(Set.of("label-id-1"))).thenReturn(List.of(label("label-id-1", NAMESPACE, MUNICIPALITY_ID)));

		assertThatNoException().isThrownBy(() -> service.validateLabels(NAMESPACE, MUNICIPALITY_ID, List.of(new ErrandLabel().withId("label-id-1").withVersion(1L))));
	}

	private static Stream<Arguments> labelsOfOtherNamespaces() {
		return Stream.of(
			arguments("otherNamespace", MUNICIPALITY_ID),
			arguments(NAMESPACE, "otherMunicipalityId"));
	}

	private static MetadataLabelEntity label(final String id, final String namespace, final String municipalityId) {
		return MetadataLabelEntity.create().withId(id).withNamespace(namespace).withMunicipalityId(municipalityId);
	}
}
