package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.CATEGORY;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;

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
	void createStatus() {

		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var status = Status.create().withName(name);

		// Mock
		when(statusRepositoryMock.save(any())).thenReturn(StatusEntity.create().withName(name));

		// Call
		assertThat(metadataService.createStatus(namespace, municipalityId, status)).isEqualTo(name);

		// Verifications
		verify(statusRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(statusRepositoryMock).save(any());
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void createExistingStatus() {

		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var status = Status.create().withName(name);

		// Mock
		when(statusRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.createStatus(namespace, municipalityId, status));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Status 'name' already exists in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(statusRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(statusRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void getStatus() {
		// Setup
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(statusRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);
		when(statusRepositoryMock.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(StatusEntity.create().withName(name));

		// Call
		final var status = metadataService.getStatus(namespace, municipalityId, name);

		// Verifications
		assertThat(status.getName()).isEqualTo(name);
		verify(statusRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(statusRepositoryMock).getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(statusRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void getNonExistingStatus() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.getStatus(namespace, municipalityId, name));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Status 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(statusRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(statusRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findStatuses() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var statusEntityList = List.of(
			StatusEntity.create().withName("STATUS_3"),
			StatusEntity.create().withName("STATUS_1"),
			StatusEntity.create().withName("STATUS_2"));

		// Mock
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusEntityList);

		// Call
		final var result = metadataService.findStatuses(namespace, municipalityId);

		// Verifications
		assertThat(result).hasSize(3).extracting(Status::getName).containsExactly("STATUS_1", "STATUS_2", "STATUS_3");
		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void deleteStatus() {

		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(statusRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(any(), any(), any())).thenReturn(true);

		// Call
		metadataService.deleteStatus(namespace, municipalityId, name);

		// Verifications
		verify(statusRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(statusRepositoryMock).deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void deleteNonExistingStatus() {

		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.deleteStatus(namespace, municipalityId, name));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Status 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(statusRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(statusRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findCategories() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryEntityList = List.of(
			CategoryEntity.create().withName("CATEGORY_2").withDisplayName("Category-2"),
			CategoryEntity.create().withName("CATEGORY_3").withDisplayName("Category-3"),
			CategoryEntity.create().withName("CATEGORY_1").withDisplayName("Category-1"));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);

		// Call
		final var result = metadataService.findCategories(namespace, municipalityId);

		// Verifications
		assertThat(result).hasSize(3).extracting(Category::getName).containsExactly("CATEGORY_1", "CATEGORY_2", "CATEGORY_3");
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findTypesForCategory() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var category = "CATEGORY_2";
		final var categoryEntityList = List.of(
			CategoryEntity.create().withName("CATEGORY_1").withTypes(List.of(TypeEntity.create().withName("TYPE_1"), TypeEntity.create().withName("TYPE_2"))),
			CategoryEntity.create().withName(category).withTypes(List.of(
				TypeEntity.create().withName("TYPE_5").withDisplayName("Type-5"),
				TypeEntity.create().withName("TYPE_3").withDisplayName("Type-3"),
				TypeEntity.create().withName("TYPE_4").withDisplayName("Type-4"))));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);

		// Call
		final var result = metadataService.findTypes(namespace, municipalityId, category);

		// Verifications
		assertThat(result).hasSize(3).extracting(Type::getName).containsExactly("TYPE_3", "TYPE_4", "TYPE_5");
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findExternalIdTypes() {

		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var externalIdTypeEntityList = List.of(
			ExternalIdTypeEntity.create().withName("EXTERNALIDTYPE-3"),
			ExternalIdTypeEntity.create().withName("EXTERNALIDTYPE-1"),
			ExternalIdTypeEntity.create().withName("EXTERNALIDTYPE-2"));

		// Mock
		when(externalIdTypeRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(externalIdTypeEntityList);

		// Call
		final var result = metadataService.findExternalIdTypes(namespace, municipalityId);

		// Verifications
		assertThat(result).hasSize(3).extracting(ExternalIdType::getName).containsExactly("EXTERNALIDTYPE-1", "EXTERNALIDTYPE-2", "EXTERNALIDTYPE-3");
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
