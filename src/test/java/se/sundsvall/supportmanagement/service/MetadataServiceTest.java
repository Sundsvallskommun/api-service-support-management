package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.PhaseRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.CATEGORY;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {

	@Mock
	private CategoryRepository categoryRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ExternalIdTypeRepository externalIdTypeRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private RoleRepository roleRepositoryMock;

	@Mock
	private StatusRepository statusRepositoryMock;

	@Mock
	private ValidationRepository validationRepositoryMock;

	@Mock
	private ContactReasonRepository contactReasonRepositoryMock;

	@Mock
	private PhaseRepository phaseRepositoryMock;

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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, validationRepositoryMock, statusRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);

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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
	}

	@Test
	void updateExistingCategory() {
		// Setup
		final var name = "name";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var entity = CategoryEntity.create()
			.withId("b82bd8ac-1507-4d9a-958d-369261eecc15")
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, externalIdTypeRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock, validationRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(statusRepositoryMock, categoryRepositoryMock, validationRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock);
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
		verifyNoInteractions(categoryRepositoryMock, statusRepositoryMock, validationRepositoryMock, metadataLabelRepositoryMock, roleRepositoryMock);
	}

	// =================================================================
	// Label tests
	// =================================================================

	@Test
	void createLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var label = Label.create().withResourceName("name");

		// Call
		metadataService.createLabels(namespace, municipalityId, List.of(label));

		// Verifications
		verify(metadataLabelRepositoryMock).saveAll(any());
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void updateLabels() {
		// Setup — all existing labels are kept (no removals)
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var labelId = "label-id";
		final var label = Label.create().withId(labelId).withResourceName("name").withClassification("updated_class").withDisplayName("Updated");
		final var existingEntity = MetadataLabelEntity.create().withId(labelId).withResourceName("name").withClassification("old_class").withDisplayName("Old")
			.withMunicipalityId(municipalityId).withNamespace(namespace);

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(List.of(existingEntity));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId))
			.thenReturn(new ArrayList<>(List.of(existingEntity)));

		metadataService.updateLabels(namespace, municipalityId, List.of(label));

		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId);
		verify(metadataLabelRepositoryMock).flush();
		verify(metadataLabelRepositoryMock).saveAll(any());
		verifyNoInteractions(errandsRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void updateLabelsNoExistingLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var labels = List.of(Label.create().withResourceName("name"));
		// Mock
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(emptyList());
		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.updateLabels(namespace, municipalityId, labels));
		// Verifications
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Labels are not present in namespace 'namespace' for municipalityId 'municipalityId'");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(metadataLabelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void updateLabelsDeletesRemovedRootLabel() {
		// Setup: two existing root labels, incoming request only has one — the other should be deleted
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var keepId = "keep-id";
		final var deleteId = "delete-id";
		final var keepLabel = Label.create().withId(keepId).withResourceName("keep");
		final var existingKeep = MetadataLabelEntity.create().withId(keepId).withResourceName("keep")
			.withMunicipalityId(municipalityId).withNamespace(namespace);
		final var existingDelete = MetadataLabelEntity.create().withId(deleteId).withResourceName("delete")
			.withMunicipalityId(municipalityId).withNamespace(namespace);

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(List.of(existingKeep, existingDelete));
		when(errandsRepositoryMock.existsByLabelsMetadataLabelIdIn(any())).thenReturn(false);
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId))
			.thenReturn(new ArrayList<>(List.of(existingKeep, existingDelete)));

		metadataService.updateLabels(namespace, municipalityId, List.of(keepLabel));

		verify(errandsRepositoryMock).existsByLabelsMetadataLabelIdIn(any());
		verify(metadataLabelRepositoryMock).deleteById(deleteId);
		verify(metadataLabelRepositoryMock, never()).deleteById(keepId);
		verify(metadataLabelRepositoryMock).flush();
		verify(metadataLabelRepositoryMock).saveAll(any());
	}

	@Test
	void updateLabelsDeletesRemovedChildLabel() {
		// Setup: existing root with two children, incoming request only has one child — the other child should be removed
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var rootId = "root-id";
		final var keepChildId = "keep-child-id";
		final var deleteChildId = "delete-child-id";

		final var keepChildLabel = Label.create().withId(keepChildId).withResourceName("keep_child");
		final var rootLabel = Label.create().withId(rootId).withResourceName("root").withLabels(List.of(keepChildLabel));

		final var existingKeepChild = MetadataLabelEntity.create().withId(keepChildId).withResourceName("keep_child")
			.withMunicipalityId(municipalityId).withNamespace(namespace);
		final var existingDeleteChild = MetadataLabelEntity.create().withId(deleteChildId).withResourceName("delete_child")
			.withMunicipalityId(municipalityId).withNamespace(namespace);
		final var existingRoot = MetadataLabelEntity.create().withId(rootId).withResourceName("root")
			.withMunicipalityId(municipalityId).withNamespace(namespace)
			.withMetadataLabels(new ArrayList<>(List.of(existingKeepChild, existingDeleteChild)));

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(List.of(existingRoot, existingKeepChild, existingDeleteChild));
		when(errandsRepositoryMock.existsByLabelsMetadataLabelIdIn(any())).thenReturn(false);
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId))
			.thenReturn(new ArrayList<>(List.of(existingRoot)));

		metadataService.updateLabels(namespace, municipalityId, List.of(rootLabel));

		// Root is not deleted — child removal handled by updateMetadataLabelEntities + orphanRemoval
		verify(errandsRepositoryMock).existsByLabelsMetadataLabelIdIn(any());
		verify(metadataLabelRepositoryMock, never()).deleteById(any());
		verify(metadataLabelRepositoryMock).flush();
		verify(metadataLabelRepositoryMock).saveAll(any());
		// Verify the deleted child was removed from the root's children list
		assertThat(existingRoot.getMetadataLabels()).hasSize(1);
		assertThat(existingRoot.getMetadataLabels().getFirst().getId()).isEqualTo(keepChildId);
	}

	@Test
	void updateLabelsBlockedByReferencedErrand() {
		// Setup: trying to remove a label that is referenced by an errand
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var keepId = "keep-id";
		final var referencedId = "referenced-id";
		final var keepLabel = Label.create().withId(keepId).withResourceName("keep");

		final var existingKeep = MetadataLabelEntity.create().withId(keepId).withResourceName("keep")
			.withMunicipalityId(municipalityId).withNamespace(namespace);
		final var existingReferenced = MetadataLabelEntity.create().withId(referencedId).withResourceName("referenced")
			.withMunicipalityId(municipalityId).withNamespace(namespace);

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(List.of(existingKeep, existingReferenced));
		when(errandsRepositoryMock.existsByLabelsMetadataLabelIdIn(any())).thenReturn(true);

		// Call — should throw because referencedId is used by an errand
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.updateLabels(namespace, municipalityId, List.of(keepLabel)));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).contains("Cannot delete labels with ids");
		assertThat(e.getMessage()).contains(referencedId);
		verify(errandsRepositoryMock).existsByLabelsMetadataLabelIdIn(any());
		verifyNoMoreInteractions(errandsRepositoryMock);
	}

	@Test
	void updateLabelsBlockedByReferencedChildErrand() {
		// Setup: trying to remove a child label that is referenced by an errand
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var rootId = "root-id";
		final var keepChildId = "keep-child-id";
		final var referencedChildId = "referenced-child-id";

		final var keepChildLabel = Label.create().withId(keepChildId).withResourceName("keep-child");
		final var rootLabel = Label.create().withId(rootId).withResourceName("root").withLabels(List.of(keepChildLabel));

		final var existingRoot = MetadataLabelEntity.create().withId(rootId).withResourceName("root")
			.withMunicipalityId(municipalityId).withNamespace(namespace);
		final var existingKeepChild = MetadataLabelEntity.create().withId(keepChildId).withResourceName("keep-child")
			.withMunicipalityId(municipalityId).withNamespace(namespace);
		final var existingReferencedChild = MetadataLabelEntity.create().withId(referencedChildId).withResourceName("referenced-child")
			.withMunicipalityId(municipalityId).withNamespace(namespace);

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(List.of(existingRoot, existingKeepChild, existingReferencedChild));
		when(errandsRepositoryMock.existsByLabelsMetadataLabelIdIn(any())).thenReturn(true);

		// Call — should throw because referencedChildId is used by an errand
		final var e = assertThrows(ThrowableProblem.class, () -> metadataService.updateLabels(namespace, municipalityId, List.of(rootLabel)));

		// Verifications
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).contains("Cannot delete labels with ids");
		assertThat(e.getMessage()).contains(referencedChildId);
		verify(errandsRepositoryMock).existsByLabelsMetadataLabelIdIn(any());
		verifyNoMoreInteractions(errandsRepositoryMock);
	}

	@Test
	void getLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId)).thenReturn(List.of(MetadataLabelEntity.create()));

		// Call
		final var labels = metadataService.findLabels(namespace, municipalityId);

		// Verifications
		assertThat(labels.getLabelStructure()).hasSize(1);

		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId);
		verifyNoMoreInteractions(metadataLabelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void getNonExistingLabels() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId)).thenReturn(emptyList());

		// Call
		final var result = metadataService.findLabels(namespace, municipalityId);

		// Verifications
		assertThat(result).isNull();

		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId);
		verifyNoMoreInteractions(metadataLabelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void deleteLabels() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		// Mock
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(true);
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId)).thenReturn(List.of(
			MetadataLabelEntity.create().withId("id-1"),
			MetadataLabelEntity.create().withId("id-2"),
			MetadataLabelEntity.create().withId("id-3")));

		// Call
		metadataService.deleteLabels(namespace, municipalityId);

		// Verifications
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId);
		verify(metadataLabelRepositoryMock).deleteById("id-1");
		verify(metadataLabelRepositoryMock).deleteById("id-2");
		verify(metadataLabelRepositoryMock).deleteById("id-3");
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
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(metadataLabelRepositoryMock);
		verifyNoInteractions(categoryRepositoryMock, externalIdTypeRepositoryMock, roleRepositoryMock, validationRepositoryMock, statusRepositoryMock);
	}

	@Test
	void patternToLabelsReturnsMatches() {
		// Setup
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var patterns = List.of("path/TO/resource/**", "path/other/**");

		final var entity1 = MetadataLabelEntity.create().withResourcePath("path/to/resource/file1");
		final var entity2 = MetadataLabelEntity.create().withResourcePath("path/to/resource");
		final var entity3 = MetadataLabelEntity.create().withResourcePath("path/other/resource/fileX");
		final var entity4 = MetadataLabelEntity.create().withResourcePath("path/some/resource/fileX");
		final var dbResults = List.of(entity1, entity2, entity3, entity4);

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId))
			.thenReturn(dbResults);

		// Call
		final var result = metadataService.patternToLabels(namespace, municipalityId, patterns);

		// Verifications
		assertThat(result).hasSize(3).containsExactlyInAnyOrder(entity1, entity2, entity3);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(metadataLabelRepositoryMock);
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
		final var metadataLabelEntity = List.of(MetadataLabelEntity.create().withResourceName("LABEL-1"));

		// Mock
		when(categoryRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(categoryEntityList);
		when(externalIdTypeRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(externalIdTypeEntityList);
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndParentIsNull(any(), any())).thenReturn(metadataLabelEntity);
		when(statusRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(statusEntityList);
		when(roleRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(roleEntityList);
		when(contactReasonRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(contactReasonEntityList);
		when(phaseRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(emptyList());

		// Call
		final var result = metadataService.findAll(namespace, municipalityId);

		// Verifications
		assertThat(result).isNotNull();
		assertThat(result.getCategories()).hasSize(3).extracting(Category::getName).containsExactlyInAnyOrder("CATEGORY-1", "CATEGORY-2", "CATEGORY-3");
		result.getCategories().forEach(category -> assertThat(category.getTypes()).hasSize(3).extracting(Type::getName).containsExactlyInAnyOrder("TYPE-1", "TYPE-2", "TYPE-3"));
		assertThat(result.getExternalIdTypes()).hasSize(2).extracting(ExternalIdType::getName).containsExactlyInAnyOrder("EXTERNALIDTYPE-1", "EXTERNALIDTYPE-2");
		assertThat(result.getLabels().getLabelStructure()).hasSize(1).extracting(Label::getResourceName).containsExactly("LABEL-1");
		assertThat(result.getRoles()).hasSize(3).extracting(Role::getName).containsExactlyInAnyOrder("ROLE-1", "ROLE-2", "ROLE-3");
		assertThat(result.getStatuses()).hasSize(3).extracting(Status::getName).containsExactlyInAnyOrder("STATUS-1", "STATUS-2", "STATUS-3");
		assertThat(result.getContactReasons()).hasSize(2).extracting(ContactReason::getReason).containsExactlyInAnyOrder("CONTACTREASON-1", "CONTACTREASON-2");

		verify(categoryRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(externalIdTypeRepositoryMock).findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId);
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

	// =================================================================
	// Phase tests
	// =================================================================

	@Test
	void createPhase() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var phase = new se.sundsvall.supportmanagement.api.model.metadata.Phase();
		phase.setName("INVESTIGATION");

		when(phaseRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(any(), any(), any())).thenReturn(false);
		when(phaseRepositoryMock.save(any())).thenAnswer(invocation -> {
			final var entity = (se.sundsvall.supportmanagement.integration.db.model.PhaseEntity) invocation.getArgument(0);
			entity.setId("generated-id");
			return entity;
		});

		final var result = metadataService.createPhase(namespace, municipalityId, phase);

		assertThat(result).isEqualTo("generated-id");
		verify(phaseRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, "INVESTIGATION");
		verify(phaseRepositoryMock).save(any());
	}

	@Test
	void createPhaseAlreadyExists() {
		when(phaseRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(any(), any(), any())).thenReturn(true);

		final var phase = new se.sundsvall.supportmanagement.api.model.metadata.Phase();
		phase.setName("EXISTING");

		assertThrows(ThrowableProblem.class, () -> metadataService.createPhase("namespace", "municipalityId", phase));
	}

	@Test
	void getPhase() {
		final var phaseEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create()
			.withId("phase-id").withName("PHASE").withDisplayName("Fas").withNamespace("namespace").withMunicipalityId("municipalityId");
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(java.util.Optional.of(phaseEntity));

		final var result = metadataService.getPhase("namespace", "municipalityId", "phase-id");

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("phase-id");
		assertThat(result.getName()).isEqualTo("PHASE");
	}

	@Test
	void getPhaseNotFound() {
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(java.util.Optional.empty());

		assertThrows(ThrowableProblem.class, () -> metadataService.getPhase("namespace", "municipalityId", "phaseId"));
	}

	@Test
	void findPhasesReturnsEmptyList() {
		when(phaseRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(emptyList());

		assertThat(metadataService.findPhases("namespace", "municipalityId")).isEmpty();
		verify(phaseRepositoryMock).findAllByNamespaceAndMunicipalityId("namespace", "municipalityId");
	}

	@Test
	void findPhasesReturnsSortedList() {
		final var phase1 = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create().withId("1").withName("B").withPhaseOrder(2);
		final var phase2 = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create().withId("2").withName("A").withPhaseOrder(1);
		when(phaseRepositoryMock.findAllByNamespaceAndMunicipalityId(any(), any())).thenReturn(List.of(phase1, phase2));

		final var result = metadataService.findPhases("namespace", "municipalityId");

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getName()).isEqualTo("A");
	}

	@Test
	void patchPhase() {
		final var phaseEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create()
			.withId("phase-id").withName("PHASE").withNamespace("namespace").withMunicipalityId("municipalityId");
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(java.util.Optional.of(phaseEntity));
		when(phaseRepositoryMock.save(any())).thenReturn(phaseEntity);

		final var patch = new se.sundsvall.supportmanagement.api.model.metadata.Phase();
		patch.setDisplayName("Updated");
		final var result = metadataService.patchPhase("phase-id", "namespace", "municipalityId", patch);

		assertThat(result).isNotNull();
		verify(phaseRepositoryMock).save(any());
	}

	@Test
	void patchPhaseNotFound() {
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(java.util.Optional.empty());

		assertThrows(ThrowableProblem.class, () -> metadataService.patchPhase("phaseId", "namespace", "municipalityId", new se.sundsvall.supportmanagement.api.model.metadata.Phase()));
	}

	@Test
	void deletePhaseNotFound() {
		when(phaseRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(false);

		assertThrows(ThrowableProblem.class, () -> metadataService.deletePhase("phaseId", "namespace", "municipalityId"));
	}

	@Test
	void deletePhase() {
		when(phaseRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.existsByPhasesPhaseEntityId(any())).thenReturn(false);

		metadataService.deletePhase("phaseId", "namespace", "municipalityId");

		verify(errandsRepositoryMock).existsByPhasesPhaseEntityId("phaseId");
		verify(phaseRepositoryMock).deleteByIdAndNamespaceAndMunicipalityId("phaseId", "namespace", "municipalityId");
	}

	@Test
	void deletePhaseReferencedByErrand() {
		when(phaseRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.existsByPhasesPhaseEntityId(any())).thenReturn(true);

		assertThrows(ThrowableProblem.class, () -> metadataService.deletePhase("phaseId", "namespace", "municipalityId"));

		verify(errandsRepositoryMock).existsByPhasesPhaseEntityId("phaseId");
		verify(phaseRepositoryMock, never()).deleteByIdAndNamespaceAndMunicipalityId(any(), any(), any());
	}

	// =================================================================
	// Phase Transition tests
	// =================================================================

	@Test
	void createPhaseTransition() {
		final var phaseEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create()
			.withId("phase-id").withName("PHASE").withNamespace("namespace").withMunicipalityId("municipalityId");
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId("phase-id", "namespace", "municipalityId")).thenReturn(java.util.Optional.of(phaseEntity));
		when(phaseRepositoryMock.existsByIdAndNamespaceAndMunicipalityId("target-id", "namespace", "municipalityId")).thenReturn(true);
		when(phaseRepositoryMock.save(any())).thenReturn(phaseEntity);

		final var transition = new se.sundsvall.supportmanagement.api.model.metadata.PhaseTransition();
		transition.setTargetPhaseId("target-id");

		metadataService.createPhaseTransition("namespace", "municipalityId", "phase-id", transition);

		verify(phaseRepositoryMock).save(any());
	}

	@Test
	void createPhaseTransitionPhaseNotFound() {
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(java.util.Optional.empty());

		assertThrows(ThrowableProblem.class, () -> metadataService.createPhaseTransition("namespace", "municipalityId", "phaseId", new se.sundsvall.supportmanagement.api.model.metadata.PhaseTransition()));
	}

	@Test
	void createPhaseTransitionTargetNotFound() {
		final var phaseEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create().withId("phase-id");
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId("phase-id", "namespace", "municipalityId")).thenReturn(java.util.Optional.of(phaseEntity));
		when(phaseRepositoryMock.existsByIdAndNamespaceAndMunicipalityId("bad-target", "namespace", "municipalityId")).thenReturn(false);

		final var transition = new se.sundsvall.supportmanagement.api.model.metadata.PhaseTransition();
		transition.setTargetPhaseId("bad-target");

		assertThrows(ThrowableProblem.class, () -> metadataService.createPhaseTransition("namespace", "municipalityId", "phase-id", transition));
	}

	@Test
	void findPhaseTransitions() {
		final var phaseEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create()
			.withId("phase-id").withName("PHASE");
		final var transitionEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseTransitionEntity.create()
			.withId("t-id").withTargetPhaseId("target-id").withPhaseEntity(phaseEntity);
		phaseEntity.setTransitions(new java.util.ArrayList<>(List.of(transitionEntity)));
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId("phase-id", "namespace", "municipalityId")).thenReturn(java.util.Optional.of(phaseEntity));

		final var result = metadataService.findPhaseTransitions("namespace", "municipalityId", "phase-id");

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("t-id");
	}

	@Test
	void findPhaseTransitionsPhaseNotFound() {
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(java.util.Optional.empty());

		assertThrows(ThrowableProblem.class, () -> metadataService.findPhaseTransitions("namespace", "municipalityId", "phaseId"));
	}

	@Test
	void deletePhaseTransition() {
		final var phaseEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create().withId("phase-id");
		final var transitionEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseTransitionEntity.create()
			.withId("t-id").withTargetPhaseId("target-id").withPhaseEntity(phaseEntity);
		phaseEntity.setTransitions(new java.util.ArrayList<>(List.of(transitionEntity)));
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId("phase-id", "namespace", "municipalityId")).thenReturn(java.util.Optional.of(phaseEntity));
		when(phaseRepositoryMock.save(any())).thenReturn(phaseEntity);

		metadataService.deletePhaseTransition("namespace", "municipalityId", "phase-id", "t-id");

		verify(phaseRepositoryMock).save(any());
	}

	@Test
	void deletePhaseTransitionNotFound() {
		final var phaseEntity = se.sundsvall.supportmanagement.integration.db.model.PhaseEntity.create().withId("phase-id");
		phaseEntity.setTransitions(new java.util.ArrayList<>());
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId("phase-id", "namespace", "municipalityId")).thenReturn(java.util.Optional.of(phaseEntity));

		assertThrows(ThrowableProblem.class, () -> metadataService.deletePhaseTransition("namespace", "municipalityId", "phase-id", "nonexistent"));
	}

	@Test
	void deletePhaseTransitionPhaseNotFound() {
		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(java.util.Optional.empty());

		assertThrows(ThrowableProblem.class, () -> metadataService.deletePhaseTransition("namespace", "municipalityId", "phaseId", "transitionId"));
	}
}
