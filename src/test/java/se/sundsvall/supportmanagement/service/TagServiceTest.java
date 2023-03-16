package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.CATEGORY;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

	@Mock
	private StatusRepository statusRepositoryMock;

	@Mock
	private CategoryRepository categoryRepositoryMock;

	@Mock
	private ExternalIdTypeRepository externalIdTypeRepositoryMock;

	@Mock
	private ValidationRepository validationRepositoryMock;

	@InjectMocks
	private TagService tagService;

	@Test
	void findAllStatusTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var statusTagEntityList = List.of(
			StatusEntity.create().withName("Status-1"),
			StatusEntity.create().withName("Status-2"),
			StatusEntity.create().withName("Status-3"));

		// Mock
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusTagEntityList);

		// Call
		final var result = tagService.findAllStatusTags(namespace, municipalityId);

		// Verifications
		assertThat(result).containsExactly("Status-1", "Status-2", "Status-3");
		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock);
	}

	@Test
	void findAllCategoryTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryTagEntityList = List.of(
			CategoryEntity.create().withName("Category-1"),
			CategoryEntity.create().withName("Category-2"),
			CategoryEntity.create().withName("Category-3"));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryTagEntityList);

		// Call
		final var result = tagService.findAllCategoryTags(namespace, municipalityId);

		// Verifications
		assertThat(result).containsExactly("Category-1", "Category-2", "Category-3");
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock);
	}

	@Test
	void findAllTypeTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var category = "Category-2";
		final var categoryTagEntityList = List.of(
			CategoryEntity.create().withName("Category-1").withTypes(List.of(TypeEntity.create().withName("Type-1"), TypeEntity.create().withName("Type-2"))),
			CategoryEntity.create().withName(category).withTypes(List.of(TypeEntity.create().withName("Type-3"), TypeEntity.create().withName("Type-4"))));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryTagEntityList);

		// Call
		final var result = tagService.findAllTypeTags(namespace, municipalityId, category);

		// Verifications
		assertThat(result).containsExactly("Type-3", "Type-4");
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock);
	}

	@Test
	void findAllExternalIdTypeTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryTagEntityList = List.of(
			ExternalIdTypeEntity.create().withName("ExternalIdTypeTag-1"),
			ExternalIdTypeEntity.create().withName("ExternalIdTypeTag-2"));

		// Mock
		when(externalIdTypeRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryTagEntityList);

		// Call
		final var result = tagService.findAllExternalIdTypeTags(namespace, municipalityId);

		// Verifications
		assertThat(result).containsExactly("ExternalIdTypeTag-1", "ExternalIdTypeTag-2");
		verify(externalIdTypeRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock);
	}

	@Test
	void findAllTags() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var statusTagEntityList = List.of(
			StatusEntity.create().withName("STATUS-1"),
			StatusEntity.create().withName("STATUS-2"),
			StatusEntity.create().withName("STATUS-3"));
		final var typeEntityList = List.of(
			TypeEntity.create().withName("TYPE-1"),
			TypeEntity.create().withName("TYPE-2"),
			TypeEntity.create().withName("TYPE-3"));
		final var categoryTagEntityList = List.of(
			CategoryEntity.create().withName("CATEGORY-1").withTypes(typeEntityList),
			CategoryEntity.create().withName("CATEGORY-2").withTypes(typeEntityList),
			CategoryEntity.create().withName("CATEGORY-3").withTypes(typeEntityList));

		// Mock
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusTagEntityList);
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryTagEntityList);

		// Call
		final var result = tagService.findAllTags(namespace, municipalityId);

		// Verifications
		assertThat(result).isNotNull();
		assertThat(result.getStatusTags()).containsExactly("STATUS-1", "STATUS-2", "STATUS-3");
		assertThat(result.getCategoryTags()).containsExactly("CATEGORY-1", "CATEGORY-2", "CATEGORY-3");
		assertThat(result.getTypeTags()).containsExactly("TYPE-1", "TYPE-2", "TYPE-3");

		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(categoryRepositoryMock, times(2)).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(externalIdTypeRepositoryMock);
	}

	@Test
	void isValidated() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var type = CATEGORY;
		final var validationEntity = ValidationEntity.create().withType(type).withValidated(true);

		// Mock
		when(validationRepositoryMock.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).thenReturn(Optional.of(validationEntity));

		// Call and assert
		assertThat(tagService.isValidated(namespace, municipalityId, type)).isTrue();
		verify(validationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
	}

	@Test
	void isNotValidated() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var type = CATEGORY;
		final var validationEntity = ValidationEntity.create().withType(type);

		// Mock
		when(validationRepositoryMock.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).thenReturn(Optional.of(validationEntity));

		// Call and assert
		assertThat(tagService.isValidated(namespace, municipalityId, type)).isFalse();
		verify(validationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
	}

	@Test
	void typeIsNotPresentInResult() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var type = CATEGORY;

		// Mock
		when(validationRepositoryMock.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).thenReturn(Optional.empty());

		// Call and assert
		assertThat(tagService.isValidated(namespace, municipalityId, type)).isFalse();
		verify(validationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
	}
}
