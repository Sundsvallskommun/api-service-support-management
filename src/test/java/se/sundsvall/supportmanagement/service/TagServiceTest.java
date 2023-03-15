package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.TagType.CATEGORY;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.CategoryTagRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeTagRepository;
import se.sundsvall.supportmanagement.integration.db.StatusTagRepository;
import se.sundsvall.supportmanagement.integration.db.TagValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryTagEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeTagEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusTagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TagValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeTagEntity;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

	@Mock
	private StatusTagRepository statusTagRepositoryMock;

	@Mock
	private CategoryTagRepository categoryTagTagRepositoryMock;

	@Mock
	private ExternalIdTypeTagRepository externalIdTypeTagRepositoryMock;

	@Mock
	private TagValidationRepository tagValidationRepositoryMock;

	@InjectMocks
	private TagService tagService;

	@Test
	void findAllStatusTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var statusTagEntityList = List.of(
			StatusTagEntity.create().withName("Status-1"),
			StatusTagEntity.create().withName("Status-2"),
			StatusTagEntity.create().withName("Status-3"));

		// Mock
		when(statusTagRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusTagEntityList);

		// Call
		final var result = tagService.findAllStatusTags(namespace, municipalityId);

		// Verifications
		assertThat(result).containsExactly("Status-1", "Status-2", "Status-3");
		verify(statusTagRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(categoryTagTagRepositoryMock, externalIdTypeTagRepositoryMock);
	}

	@Test
	void findAllCategoryTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryTagEntityList = List.of(
			CategoryTagEntity.create().withName("Category-1"),
			CategoryTagEntity.create().withName("Category-2"),
			CategoryTagEntity.create().withName("Category-3"));

		// Mock
		when(categoryTagTagRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryTagEntityList);

		// Call
		final var result = tagService.findAllCategoryTags(namespace, municipalityId);

		// Verifications
		assertThat(result).containsExactly("Category-1", "Category-2", "Category-3");
		verify(categoryTagTagRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusTagRepositoryMock, externalIdTypeTagRepositoryMock);
	}

	@Test
	void findAllTypeTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var category = "Category-2";
		final var categoryTagEntityList = List.of(
			CategoryTagEntity.create().withName("Category-1").withTypeTags(List.of(TypeTagEntity.create().withName("Type-1"), TypeTagEntity.create().withName("Type-2"))),
			CategoryTagEntity.create().withName(category).withTypeTags(List.of(TypeTagEntity.create().withName("Type-3"), TypeTagEntity.create().withName("Type-4"))));

		// Mock
		when(categoryTagTagRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryTagEntityList);

		// Call
		final var result = tagService.findAllTypeTags(namespace, municipalityId, category);

		// Verifications
		assertThat(result).containsExactly("Type-3", "Type-4");
		verify(categoryTagTagRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusTagRepositoryMock, externalIdTypeTagRepositoryMock);
	}

	@Test
	void findAllExternalIdTypeTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryTagEntityList = List.of(
			ExternalIdTypeTagEntity.create().withName("ExternalIdTypeTag-1"),
			ExternalIdTypeTagEntity.create().withName("ExternalIdTypeTag-2"));

		// Mock
		when(externalIdTypeTagRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryTagEntityList);

		// Call
		final var result = tagService.findAllExternalIdTypeTags(namespace, municipalityId);

		// Verifications
		assertThat(result).containsExactly("ExternalIdTypeTag-1", "ExternalIdTypeTag-2");
		verify(externalIdTypeTagRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusTagRepositoryMock, categoryTagTagRepositoryMock);
	}

	@Test
	void findAllTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var statusTagEntityList = List.of(
			StatusTagEntity.create().withName("STATUS-1"),
			StatusTagEntity.create().withName("STATUS-2"),
			StatusTagEntity.create().withName("STATUS-3"));
		final var typeEntityList = List.of(
			TypeTagEntity.create().withName("TYPE-1"),
			TypeTagEntity.create().withName("TYPE-2"),
			TypeTagEntity.create().withName("TYPE-3"));
		final var categoryTagEntityList = List.of(
			CategoryTagEntity.create().withName("CATEGORY-1").withTypeTags(typeEntityList),
			CategoryTagEntity.create().withName("CATEGORY-2").withTypeTags(typeEntityList),
			CategoryTagEntity.create().withName("CATEGORY-3").withTypeTags(typeEntityList));

		// Mock
		when(statusTagRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusTagEntityList);
		when(categoryTagTagRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryTagEntityList);

		// Call
		final var result = tagService.findAllTags(namespace, municipalityId);

		// Verifications
		assertThat(result).isNotNull();
		assertThat(result.getStatusTags()).containsExactly("STATUS-1", "STATUS-2", "STATUS-3");
		assertThat(result.getCategoryTags()).containsExactly("CATEGORY-1", "CATEGORY-2", "CATEGORY-3");
		assertThat(result.getTypeTags()).containsExactly("TYPE-1", "TYPE-2", "TYPE-3");

		verify(statusTagRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(categoryTagTagRepositoryMock, times(2)).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(externalIdTypeTagRepositoryMock);
	}

	@Test
	void isValidated() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var type = CATEGORY;
		final var validationEntity = TagValidationEntity.create().withType(type).withValidated(true);

		// Mock
		when(tagValidationRepositoryMock.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).thenReturn(Optional.of(validationEntity));

		// Call and assert
		assertThat(tagService.isValidated(namespace, municipalityId, type)).isTrue();
		verify(tagValidationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
	}

	@Test
	void isNotValidated() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var type = CATEGORY;
		final var validationEntity = TagValidationEntity.create().withType(type);

		// Mock
		when(tagValidationRepositoryMock.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).thenReturn(Optional.of(validationEntity));

		// Call and assert
		assertThat(tagService.isValidated(namespace, municipalityId, type)).isFalse();
		verify(tagValidationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
	}

	@Test
	void typeIsNotPresentInResult() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var type = CATEGORY;

		// Mock
		when(tagValidationRepositoryMock.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).thenReturn(Optional.empty());

		// Call and assert
		assertThat(tagService.isValidated(namespace, municipalityId, type)).isFalse();
		verify(tagValidationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
	}
}
