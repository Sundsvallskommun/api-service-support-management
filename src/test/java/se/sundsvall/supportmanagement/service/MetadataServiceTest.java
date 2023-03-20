package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {

	@Mock
	private StatusRepository statusRepositoryMock;

	@Mock
	private CategoryRepository categoryRepositoryMock;

	@Mock
	private ExternalIdTypeRepository externalIdTypeRepositoryMock;

	@Mock
	private ValidationRepository validationRepositoryMock;

	@InjectMocks
	private MetadataService metadataService;

	@Test
	void findAllStatuses() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var statusEntityList = List.of(
			StatusEntity.create().withName("Status-1"),
			StatusEntity.create().withName("Status-2"),
			StatusEntity.create().withName("Status-3"));

		// Mock
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusEntityList);

		// Call
		final var result = metadataService.findStatuses(namespace, municipalityId);

		// Verifications
		assertThat(result).hasSize(3).extracting(Status::getName).containsExactlyInAnyOrder("Status-1", "Status-2", "Status-3");
		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findAllCategories() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryEntityList = List.of(
			CategoryEntity.create().withName("Category-1"),
			CategoryEntity.create().withName("Category-2"),
			CategoryEntity.create().withName("Category-3"));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);

		// Call
		final var result = metadataService.findCategories(namespace, municipalityId);

		// Verifications
		assertThat(result).hasSize(3).extracting(Category::getName).containsExactlyInAnyOrder("Category-1", "Category-2", "Category-3");
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findAllTypesForCategory() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var category = "Category-2";
		final var categoryEntityList = List.of(
			CategoryEntity.create().withName("Category-1").withTypes(List.of(TypeEntity.create().withName("Type-1"), TypeEntity.create().withName("Type-2"))),
			CategoryEntity.create().withName(category).withTypes(List.of(TypeEntity.create().withName("Type-3"), TypeEntity.create().withName("Type-4"))));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);

		// Call
		final var result = metadataService.findTypes(namespace, municipalityId, category);

		// Verifications
		assertThat(result).hasSize(2).extracting(Type::getName).containsExactlyInAnyOrder("Type-3", "Type-4");
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findAllExternalIdTypes() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var externalIdTypeEntityList = List.of(
			ExternalIdTypeEntity.create().withName("EXTERNALIDTYPE-1"),
			ExternalIdTypeEntity.create().withName("EXTERNALIDTYPE-2"));

		// Mock
		when(externalIdTypeRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(externalIdTypeEntityList);

		// Call
		final var result = metadataService.findExternalIdTypes(namespace, municipalityId);

		// Verifications
		assertThat(result).hasSize(2).extracting(ExternalIdType::getName).containsExactlyInAnyOrder("EXTERNALIDTYPE-1", "EXTERNALIDTYPE-2");
		verify(externalIdTypeRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findAll() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var statusEntityList = List.of(
			StatusEntity.create().withName("STATUS-1"),
			StatusEntity.create().withName("STATUS-2"),
			StatusEntity.create().withName("STATUS-3"));
		final var typeEntityList = List.of(
			TypeEntity.create().withName("TYPE-1"),
			TypeEntity.create().withName("TYPE-2"),
			TypeEntity.create().withName("TYPE-3"));
		final var categoryEntityList = List.of(
			CategoryEntity.create().withName("CATEGORY-1").withTypes(typeEntityList),
			CategoryEntity.create().withName("CATEGORY-2").withTypes(typeEntityList),
			CategoryEntity.create().withName("CATEGORY-3").withTypes(typeEntityList));
		final var externalIdTypeEntityList = List.of(
			ExternalIdTypeEntity.create().withName("EXTERNALIDTYPE-1"),
			ExternalIdTypeEntity.create().withName("EXTERNALIDTYPE-2"));

		// Mock
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusEntityList);
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);
		when(externalIdTypeRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(externalIdTypeEntityList);

		// Call
		final var result = metadataService.findAll(namespace, municipalityId);

		// Verifications
		assertThat(result).isNotNull();
		assertThat(result.getStatuses()).hasSize(3).extracting(Status::getName).containsExactlyInAnyOrder("STATUS-1", "STATUS-2", "STATUS-3");
		assertThat(result.getCategories()).hasSize(3).extracting(Category::getName).containsExactlyInAnyOrder("CATEGORY-1", "CATEGORY-2", "CATEGORY-3");
		result.getCategories().stream().forEach(category -> assertThat(category.getTypes()).hasSize(3).extracting(Type::getName).containsExactlyInAnyOrder("TYPE-1", "TYPE-2", "TYPE-3"));
		assertThat(result.getExternalIdTypes()).hasSize(2).extracting(ExternalIdType::getName).containsExactlyInAnyOrder("EXTERNALIDTYPE-1", "EXTERNALIDTYPE-2");

		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(externalIdTypeRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(validationRepositoryMock);

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
		assertThat(metadataService.isValidated(namespace, municipalityId, type)).isTrue();
		verify(validationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, externalIdTypeRepositoryMock);
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
		assertThat(metadataService.isValidated(namespace, municipalityId, type)).isFalse();
		verify(validationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, externalIdTypeRepositoryMock);
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
		assertThat(metadataService.isValidated(namespace, municipalityId, type)).isFalse();
		verify(validationRepositoryMock).findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, externalIdTypeRepositoryMock);
	}
}
