package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.metadata.LabelClassification;
import se.sundsvall.supportmanagement.integration.db.LabelClassificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.LabelClassificationEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class LabelClassificationServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String CLASSIFICATION = "subtype";

	@Mock
	private LabelClassificationRepository repositoryMock;

	@Captor
	private ArgumentCaptor<LabelClassificationEntity> entityCaptor;

	@InjectMocks
	private LabelClassificationService service;

	@Test
	void createLabelClassification() {
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.createLabelClassification(NAMESPACE, MUNICIPALITY_ID, LabelClassification.create().withClassification(CLASSIFICATION).withDisplayName("Undertyp"));

		assertThat(result).isEqualTo(CLASSIFICATION);
		verify(repositoryMock).existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(entityCaptor.getValue().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entityCaptor.getValue().getClassification()).isEqualTo(CLASSIFICATION);
		assertThat(entityCaptor.getValue().getDisplayName()).isEqualTo("Undertyp");
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void createLabelClassificationThatAlreadyExists() {
		when(repositoryMock.existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION)).thenReturn(true);

		final var labelClassification = LabelClassification.create().withClassification(CLASSIFICATION);
		assertThatThrownBy(() -> service.createLabelClassification(NAMESPACE, MUNICIPALITY_ID, labelClassification))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo("Label classification 'subtype' already exists in namespace 'namespace' for municipalityId '2281'");
			});

		verify(repositoryMock).existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void getLabelClassification() {
		when(repositoryMock.findByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION))
			.thenReturn(Optional.of(LabelClassificationEntity.create().withId("id").withClassification(CLASSIFICATION).withDisplayName("Undertyp")));

		final var result = service.getLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);

		assertThat(result).isEqualTo(LabelClassification.create().withId("id").withClassification(CLASSIFICATION).withDisplayName("Undertyp"));
	}

	@Test
	void getNonExistingLabelClassification() {
		assertThatThrownBy(() -> service.getLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getDetail()).isEqualTo("Label classification 'subtype' is not present in namespace 'namespace' for municipalityId '2281'");
			});
	}

	@Test
	void findLabelClassificationsDefaultsToSortOnClassification() {
		when(repositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID, Sort.by("classification")))
			.thenReturn(List.of(LabelClassificationEntity.create().withClassification("a"), LabelClassificationEntity.create().withClassification("b")));

		final var result = service.findLabelClassifications(NAMESPACE, MUNICIPALITY_ID, Sort.unsorted());

		assertThat(result).extracting(LabelClassification::getClassification).containsExactly("a", "b");
	}

	@Test
	void findLabelClassificationsWithSort() {
		final var sort = Sort.by(Sort.Direction.DESC, "displayName");

		service.findLabelClassifications(NAMESPACE, MUNICIPALITY_ID, sort);

		verify(repositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID, sort);
	}

	@Test
	void updateLabelClassification() {
		final var entity = LabelClassificationEntity.create().withClassification(CLASSIFICATION).withDisplayName("Old");
		when(repositoryMock.findByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION)).thenReturn(Optional.of(entity));
		when(repositoryMock.save(entity)).thenReturn(entity);

		final var result = service.updateLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION, LabelClassification.create().withDisplayName("New"));

		assertThat(result.getClassification()).isEqualTo(CLASSIFICATION);
		assertThat(result.getDisplayName()).isEqualTo("New");
	}

	@Test
	void updateNonExistingLabelClassification() {
		final var patch = LabelClassification.create().withDisplayName("New");
		assertThatThrownBy(() -> service.updateLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION, patch))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(NOT_FOUND));

		verify(repositoryMock).findByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void deleteLabelClassification() {
		when(repositoryMock.existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION)).thenReturn(true);

		service.deleteLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);

		verify(repositoryMock).deleteByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);
	}

	@Test
	void deleteNonExistingLabelClassification() {
		assertThatThrownBy(() -> service.deleteLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(NOT_FOUND));

		verify(repositoryMock).existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void getClassificationDisplayNamesLeavesOutClassificationsWithoutDisplayName() {
		when(repositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID, Sort.unsorted())).thenReturn(List.of(
			LabelClassificationEntity.create().withClassification("category").withDisplayName("Kategori"),
			LabelClassificationEntity.create().withClassification("subtype")));

		final var result = service.getClassificationDisplayNames(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).containsExactly(entry("category", "Kategori"));
	}
}
