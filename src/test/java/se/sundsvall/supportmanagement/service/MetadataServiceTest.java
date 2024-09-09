package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.LabelRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.LabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {

	@Mock
	private CategoryRepository categoryRepositoryMock;

	@Mock
	private ExternalIdTypeRepository externalIdTypeRepositoryMock;

	@Mock
	private LabelRepository labelRepositoryMock;

	@Mock
	private RoleRepository roleRepositoryMock;

	@Mock
	private StatusRepository statusRepositoryMock;

	@Mock
	private ValidationRepository validationRepositoryMock;

	@Mock
	private ContactReasonRepository contactReasonRepositoryMock;

	@InjectMocks
	private MetadataService metadataService;

	// =================================================================
	// Status tests
	// =================================================================

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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
	}

	@Test
	void getStatus() {
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
	}

	// =================================================================
	// Role tests
	// =================================================================

	@Test
	void createRole() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var role = Role.create().withName(name);

		// Mock
		when(roleRepositoryMock.save(any())).thenReturn(RoleEntity.create().withName(name));

		// Call
		assertThat(metadataService.createRole(namespace, municipalityId, role)).isEqualTo(name);

		// Verifications
		verify(roleRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(roleRepositoryMock).save(any());
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void createExistingRole() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var role = Role.create().withName(name);

		// Mock
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.createRole(namespace, municipalityId, role));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Role 'name' already exists in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(roleRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(roleRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void getRole() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);
		when(roleRepositoryMock.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(RoleEntity.create().withName(name));

		// Call
		final var role = metadataService.getRole(namespace, municipalityId, name);

		// Verifications
		assertThat(role.getName()).isEqualTo(name);
		verify(roleRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(roleRepositoryMock).getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(roleRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void getNonExistingRole() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.getRole(namespace, municipalityId, name));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Role 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(roleRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(roleRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void findRoles() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var roleEntityList = List.of(
			RoleEntity.create().withName("ROLE_3"),
			RoleEntity.create().withName("ROLE_1"),
			RoleEntity.create().withName("ROLE_2"));

		// Mock
		when(roleRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(roleEntityList);

		// Call
		final var result = metadataService.findRoles(namespace, municipalityId);

		// Verifications
		assertThat(result).hasSize(3).extracting(Role::getName).containsExactly("ROLE_1", "ROLE_2", "ROLE_3");
		verify(roleRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void deleteRole() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(any(), any(), any())).thenReturn(true);

		// Call
		metadataService.deleteRole(namespace, municipalityId, name);

		// Verifications
		verify(roleRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(roleRepositoryMock).deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void deleteNonExistingRole() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.deleteRole(namespace, municipalityId, name));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Role 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(roleRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(roleRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	// =================================================================
	// Category tests
	// ================================================================

	@Test
	void createCategory() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var category = Category.create().withName(name);

		// Mock
		when(categoryRepositoryMock.save(any())).thenReturn(CategoryEntity.create().withName(name));

		// Call
		assertThat(metadataService.createCategory(namespace, municipalityId, category)).isEqualTo(name);

		// Verifications
		verify(categoryRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(categoryRepositoryMock).save(any());
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	@Test
	void createExistingCategory() {

		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var category = Category.create().withName(name);

		// Mock
		when(categoryRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.createCategory(namespace, municipalityId, category));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Category 'name' already exists in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(categoryRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(categoryRepositoryMock);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);

	}

	// =================================================================
	// ExternalIdType tests
	// ================================================================

	@Test
	void getCategory() {
		// Setup
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(categoryRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);
		when(categoryRepositoryMock.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(CategoryEntity.create().withName(name));

		// Call
		final var category = metadataService.getCategory(namespace, municipalityId, name);

		// Verifications
		assertThat(category.getName()).isEqualTo(name);
		verify(categoryRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(categoryRepositoryMock).getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(categoryRepositoryMock);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	@Test
	void getNonExistingCategory() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.getCategory(namespace, municipalityId, name));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Category 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(categoryRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(categoryRepositoryMock);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	@Test
	void deleteCategory() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(categoryRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(any(), any(), any())).thenReturn(true);

		// Call
		metadataService.deleteCategory(namespace, municipalityId, name);

		// Verifications
		verify(categoryRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(categoryRepositoryMock).deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	@Test
	void deleteNonExistingCategory() {

		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.deleteCategory(namespace, municipalityId, name));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Category 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(categoryRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(categoryRepositoryMock);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	@Test
	void updateExistingCategory() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var entity = CategoryEntity.create()
			.withId(1L)
			.withName("existing_category")
			.withTypes(List.of(TypeEntity.create().withName("EXISTING_TYPE_1"), TypeEntity.create().withName("EXISTING_TYPE_2")));

		final var category = Category.create().withName(name).withDisplayName("displayName").withTypes(List.of(
			Type.create().withName("TYPE_1").withDisplayName("Type-1"),
			Type.create().withName("TYPE_2").withDisplayName("Type-2"),
			Type.create().withName("TYPE_3").withDisplayName("Type-3"),
			Type.create().withName("TYPE_4").withDisplayName("Type-4")));

		// Mock
		when(categoryRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);
		when(categoryRepositoryMock.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(entity);
		when(categoryRepositoryMock.save(entity)).thenReturn(entity);

		// Call
		final var response = metadataService.updateCategory(namespace, municipalityId, name, category);

		// Assertions and verifications
		assertThat(response.getName()).isEqualTo(name);
		assertThat(response.getDisplayName()).isEqualTo("displayName");
		assertThat(response.getTypes()).hasSize(4).extracting(Type::getName).containsExactly("TYPE_1", "TYPE_2", "TYPE_3", "TYPE_4");
		verify(categoryRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(categoryRepositoryMock).getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(categoryRepositoryMock).save(entity);
		verifyNoMoreInteractions(categoryRepositoryMock);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	@Test
	void updateNonExistingCategory() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var category = Category.create();

		// Mock
		when(categoryRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(false);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> metadataService.updateCategory(namespace, municipalityId, name, category));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: Category 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(categoryRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(categoryRepositoryMock);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	@Test
	void findCategories() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryEntityList = List.of(
			CategoryEntity.create().withName("CATEGORY_2").withDisplayName("Category-2"),
			CategoryEntity.create().withName("CATEGORY_4"),
			CategoryEntity.create().withName("CATEGORY_3").withDisplayName("Category-3"),
			CategoryEntity.create().withName("CATEGORY_1").withDisplayName("Category-1"));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);

		// Call
		final var result = metadataService.findCategories(namespace, municipalityId);

		// Verifications
		assertThat(result).hasSize(4).extracting(Category::getName).containsExactly("CATEGORY_4", "CATEGORY_1", "CATEGORY_2", "CATEGORY_3");
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
				TypeEntity.create().withName("TYPE_6"),
				TypeEntity.create().withName("TYPE_3").withDisplayName("Type-3"),
				TypeEntity.create().withName("TYPE_4").withDisplayName("Type-4"))));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);

		// Call
		final var result = metadataService.findTypes(namespace, municipalityId, category);

		// Verifications
		assertThat(result).hasSize(4).extracting(Type::getName).containsExactly("TYPE_6", "TYPE_3", "TYPE_4", "TYPE_5");
		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, labelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	// =================================================================
	// ExternalIdType tests
	// ================================================================

	@Test
	void createExternalIdType() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var externalIdType = ExternalIdType.create().withName(name);

		// Mock
		when(externalIdTypeRepositoryMock.save(any())).thenReturn(ExternalIdTypeEntity.create().withName(name));

		// Call
		assertThat(metadataService.createExternalIdType(namespace, municipalityId, externalIdType)).isEqualTo(name);

		// Verifications
		verify(externalIdTypeRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(externalIdTypeRepositoryMock).save(any());
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, labelRepositoryMock, roleRepositoryMock);
	}

	@Test
	void createExistingExternalIdType() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var externalIdType = ExternalIdType.create().withName(name);

		// Mock
		when(externalIdTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.createExternalIdType(namespace, municipalityId, externalIdType));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: ExternalIdType 'name' already exists in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(externalIdTypeRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(externalIdTypeRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, labelRepositoryMock, roleRepositoryMock);
	}

	@Test
	void getExternalIdType() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(externalIdTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(true);
		when(externalIdTypeRepositoryMock.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)).thenReturn(ExternalIdTypeEntity.create().withName(name));

		// Call
		final var status = metadataService.getExternalIdType(namespace, municipalityId, name);

		// Verifications
		assertThat(status.getName()).isEqualTo(name);
		verify(externalIdTypeRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(externalIdTypeRepositoryMock).getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(externalIdTypeRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, labelRepositoryMock, roleRepositoryMock);
	}

	@Test
	void getNonExistingExternalIdType() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.getExternalIdType(namespace, municipalityId, name));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: ExternalIdType 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(externalIdTypeRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(externalIdTypeRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, labelRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, validationRepositoryMock, labelRepositoryMock, roleRepositoryMock);
	}

	@Test
	void deleteExternalIdType() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(externalIdTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(any(), any(), any())).thenReturn(true);

		// Call
		metadataService.deleteExternalIdType(namespace, municipalityId, name);

		// Verifications
		verify(externalIdTypeRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verify(externalIdTypeRepositoryMock).deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, labelRepositoryMock, roleRepositoryMock);
	}

	@Test
	void deleteNonExistingExternalIdType() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.deleteExternalIdType(namespace, municipalityId, name));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: ExternalIdType 'name' is not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(externalIdTypeRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
		verifyNoMoreInteractions(externalIdTypeRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, labelRepositoryMock, roleRepositoryMock);
	}

	// =================================================================
	// Label tests
	// =================================================================

	@Test
	void createLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var label = Label.create().withName("name");

		// Call
		metadataService.createLabels(namespace, municipalityId, List.of(label));

		// Verifications
		verify(labelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(labelRepositoryMock).save(any());
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void createExistingLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var label = Label.create();
		final var labels = List.of(label);

		// Mock
		when(labelRepositoryMock.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(true);

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.createLabels(namespace, municipalityId, labels));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Labels already exists in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(labelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(labelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void createLabelsNonUniqueNames() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var labels = List.of(
				Label.create().withName("name1")
						.withLabels(List.of(Label.create().withName("name2"))),
				Label.create().withName("name3"),
				Label.create().withName("name4")
						.withLabels(List.of(Label.create().withName("name1"))));

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.createLabels(namespace, municipalityId, labels));
		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Label names must be unique. Duplication detected for 'name1'");
		verify(labelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(labelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void updateLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var label = Label.create().withName("name");
		final var labelEntity = LabelEntity.create();
		// Mock
		when(labelRepositoryMock.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(true);
		when(labelRepositoryMock.findOneByNamespaceAndMunicipalityId(any(), any())).thenReturn(labelEntity);
		// Call
		metadataService.updateLabels(namespace, municipalityId, List.of(label));
		// Verifications
		verify(labelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(labelRepositoryMock).findOneByNamespaceAndMunicipalityId(namespace, municipalityId);
		ArgumentCaptor<LabelEntity> labelEntityCaptor = ArgumentCaptor.forClass(LabelEntity.class);
		verify(labelRepositoryMock).save(labelEntityCaptor.capture());
		assertThat(labelEntityCaptor.getValue()).isSameAs(labelEntity);
		assertThat(labelEntityCaptor.getValue().getJsonStructure()).isEqualTo("[{\"name\":\"name\"}]");
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void updateLabelsNonUniqueNames() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var labels = List.of(
				Label.create().withName("name1")
						.withLabels(List.of(Label.create().withName("name2"))),
				Label.create().withName("name3"),
				Label.create().withName("name4")
						.withLabels(List.of(Label.create().withName("name1"))));
		// Mock
		when(labelRepositoryMock.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(true);
		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.updateLabels(namespace, municipalityId, labels));
		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Label names must be unique. Duplication detected for 'name1'");
		verify(labelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(labelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void updateLabelsNoExistingLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var labels = List.of(Label.create().withName("name"));
		// Mock
		when(labelRepositoryMock.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(false);
		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.updateLabels(namespace, municipalityId, labels));
		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Labels dos not exists in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(labelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(labelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void getLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var json = "[{\"classification\":\"classification\",\"displayName\":\"displayName\",\"name\":\"name\"}]";

		// Mock
		when(labelRepositoryMock.findOneByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(LabelEntity.create().withJsonStructure(json));

		// Call
		final var labels = metadataService.findLabels(namespace, municipalityId);

		// Verifications
		assertThat(labels.getLabelStructure()).hasSize(1).extracting(
				Label::getClassification,
				Label::getDisplayName,
				Label::getName,
				Label::getLabels)
			.containsExactly(tuple(
				"classification",
				"displayName",
				"name",
				null));

		verify(labelRepositoryMock).findOneByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(labelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void getNonExistingLabels() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		assertThat(metadataService.findLabels(namespace, municipalityId)).isNull();

		// Verifications
		verify(labelRepositoryMock).findOneByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(roleRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void deleteLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(labelRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(true);

		// Call
		metadataService.deleteLabels(namespace, municipalityId);

		// Verifications
		verify(labelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(labelRepositoryMock).deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void deleteNonExistingLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.deleteLabels(namespace, municipalityId));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Labels are not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(labelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(labelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	// =================================================================
	// Common tests
	// ================================================================

	@Test
	void findAll() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var statusEntityList = List.of(
			StatusEntity.create().withName("STATUS-1"),
			StatusEntity.create().withName("STATUS-2"),
			StatusEntity.create().withName("STATUS-3"));
		final var roleEntityList = List.of(
			RoleEntity.create().withName("ROLE-1"),
			RoleEntity.create().withName("ROLE-2"),
			RoleEntity.create().withName("ROLE-3"));
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
		final var contactReasonEntityList = List.of(
			ContactReasonEntity.create().withReason("CONTACTREASON-1"),
			ContactReasonEntity.create().withReason("CONTACTREASON-2"));
		final var labelEntity = LabelEntity.create().withJsonStructure("[{\"classification\":\"CLASSIFICATION-1\",\"name\":\"LABEL-1\"}]");

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);
		when(externalIdTypeRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(externalIdTypeEntityList);
		when(labelRepositoryMock.findOneByNamespaceAndMunicipalityId(any(), any())).thenReturn(labelEntity);
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusEntityList);
		when(roleRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(roleEntityList);
		when(contactReasonRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(contactReasonEntityList);

		// Call
		final var result = metadataService.findAll(namespace, municipalityId);

		// Verifications
		assertThat(result).isNotNull();
		assertThat(result.getCategories()).hasSize(3).extracting(Category::getName).containsExactlyInAnyOrder("CATEGORY-1", "CATEGORY-2", "CATEGORY-3");
		result.getCategories().forEach(category -> assertThat(category.getTypes()).hasSize(3).extracting(Type::getName).containsExactlyInAnyOrder("TYPE-1", "TYPE-2", "TYPE-3"));
		assertThat(result.getExternalIdTypes()).hasSize(2).extracting(ExternalIdType::getName).containsExactlyInAnyOrder("EXTERNALIDTYPE-1", "EXTERNALIDTYPE-2");
		assertThat(result.getLabels().getLabelStructure()).hasSize(1).extracting(Label::getName).containsExactly("LABEL-1");
		assertThat(result.getRoles()).hasSize(3).extracting(Role::getName).containsExactlyInAnyOrder("ROLE-1", "ROLE-2", "ROLE-3");
		assertThat(result.getStatuses()).hasSize(3).extracting(Status::getName).containsExactlyInAnyOrder("STATUS-1", "STATUS-2", "STATUS-3");
		assertThat(result.getContactReasons()).hasSize(2).extracting(ContactReason::getReason).containsExactlyInAnyOrder("CONTACTREASON-1", "CONTACTREASON-2");

		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(externalIdTypeRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(labelRepositoryMock).findOneByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(roleRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(statusRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
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
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock);
	}

}
