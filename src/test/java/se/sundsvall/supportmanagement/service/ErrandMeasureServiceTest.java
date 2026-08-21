package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandMeasureServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errand-id";
	private static final String MEASURE_ID = "measure-id";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MeasureTypeRepository measureTypeRepositoryMock;

	@Mock
	private RoleRepository roleRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private EntityManager entityManagerMock;

	@InjectMocks
	private ErrandMeasureService service;

	@Test
	void createErrandMeasure() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(new ArrayList<>());
		final var measure = new Measure().withType("INTERVENTION").withAddedByRole("MANAGER").withGoal("goal");

		when(measureTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INTERVENTION")).thenReturn(true);
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "MANAGER")).thenReturn(true);
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);
		when(errandsRepositoryMock.save(any())).thenReturn(errandEntity);

		// Act
		service.createErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, measure);

		// Assert
		verify(measureTypeRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INTERVENTION");
		verify(roleRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "MANAGER");
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW);
		verify(entityManagerMock).lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		verify(entityManagerMock).persist(errandEntity.getMeasures().getFirst());
		verify(errandsRepositoryMock).save(errandEntity);
		assertThat(errandEntity.getMeasures()).hasSize(1);
		assertThat(errandEntity.getMeasures().getFirst().getType()).isEqualTo("INTERVENTION");
		assertThat(errandEntity.getMeasures().getFirst().getGoal()).isEqualTo("goal");
	}

	@Test
	void createErrandMeasureWithInvalidType() {

		// Arrange
		final var measure = new Measure().withType("INVALID_TYPE").withAddedByRole("MANAGER");

		when(measureTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INVALID_TYPE")).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> service.createErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, measure))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("not a valid measure type");

		verifyNoInteractions(accessControlServiceMock, errandsRepositoryMock, entityManagerMock);
	}

	@Test
	void createErrandMeasureWithInvalidRole() {

		// Arrange
		final var measure = new Measure().withType("INTERVENTION").withAddedByRole("INVALID_ROLE");

		when(measureTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INTERVENTION")).thenReturn(true);
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INVALID_ROLE")).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> service.createErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, measure))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("not a valid role");

		verifyNoInteractions(accessControlServiceMock, errandsRepositoryMock, entityManagerMock);
	}

	@Test
	void readErrandMeasure() {

		// Arrange
		final var measureEntity = MeasureEntity.create()
			.withId(MEASURE_ID)
			.withType("INTERVENTION")
			.withGoal("goal")
			.withAccept(Accept.TRUE)
			.withCreated(OffsetDateTime.now());

		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(List.of(measureEntity));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);

		// Act
		final var result = service.readErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID);

		// Assert
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.MEASURE, LR);
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(MEASURE_ID);
		assertThat(result.getType()).isEqualTo("INTERVENTION");
		assertThat(result.getGoal()).isEqualTo("goal");
		assertThat(result.getAccept()).isEqualTo("TRUE");
	}

	@Test
	void readErrandMeasureNotFound() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(List.of());

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);

		// Act & Assert
		assertThatThrownBy(() -> service.readErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("could not be found");
	}

	@Test
	void findErrandMeasures() {

		// Arrange
		final var measureEntities = List.of(
			MeasureEntity.create().withId("id-1").withType("INTERVENTION"),
			MeasureEntity.create().withId("id-2").withType("SUPPORT"));

		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(measureEntities);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);

		// Act
		final var result = service.findErrandMeasures(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assert
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.MEASURE, LR);
		assertThat(result).hasSize(2);
	}

	@Test
	void findErrandMeasuresEmpty() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);

		// Act
		final var result = service.findErrandMeasures(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void updateErrandMeasure() {

		// Arrange
		final var measureEntity = MeasureEntity.create()
			.withId(MEASURE_ID)
			.withType("INTERVENTION")
			.withGoal("old goal");

		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(new ArrayList<>(List.of(measureEntity)));
		final var measure = new Measure().withGoal("new goal");

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);
		when(errandsRepositoryMock.save(any())).thenReturn(errandEntity);

		// Act
		final var result = service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, measure);

		// Assert
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW);
		verify(entityManagerMock).lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		verify(errandsRepositoryMock).save(errandEntity);
		assertThat(result).isNotNull();
		assertThat(result.getGoal()).isEqualTo("new goal");
		assertThat(result.getType()).isEqualTo("INTERVENTION");
	}

	@Test
	void updateErrandMeasureWithType() {

		// Arrange
		final var measureEntity = MeasureEntity.create()
			.withId(MEASURE_ID)
			.withType("INTERVENTION")
			.withGoal("goal");

		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(new ArrayList<>(List.of(measureEntity)));
		final var measure = new Measure().withType("SUPPORT");

		when(measureTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "SUPPORT")).thenReturn(true);
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);
		when(errandsRepositoryMock.save(any())).thenReturn(errandEntity);

		// Act
		final var result = service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, measure);

		// Assert
		verify(measureTypeRepositoryMock).existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "SUPPORT");
		assertThat(result).isNotNull();
		assertThat(result.getType()).isEqualTo("SUPPORT");
	}

	@Test
	void updateErrandMeasureWithInvalidType() {

		// Arrange
		final var measure = new Measure().withType("INVALID_TYPE");

		when(measureTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INVALID_TYPE")).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, measure))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("not a valid measure type");

		verifyNoInteractions(accessControlServiceMock, errandsRepositoryMock, entityManagerMock);
	}

	@Test
	void updateErrandMeasureWithInvalidRole() {

		// Arrange
		final var measure = new Measure().withAddedByRole("INVALID_ROLE");

		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INVALID_ROLE")).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, measure))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("not a valid role");

		verifyNoInteractions(accessControlServiceMock, errandsRepositoryMock, entityManagerMock);
	}

	@Test
	void updateErrandMeasureNotFound() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(List.of());

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);

		// Act & Assert
		assertThatThrownBy(() -> service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, new Measure()))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("could not be found");
	}

	@Test
	void deleteErrandMeasure() {

		// Arrange
		final var measureEntity = MeasureEntity.create().withId(MEASURE_ID);
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(new ArrayList<>(List.of(measureEntity)));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);
		when(errandsRepositoryMock.save(any())).thenReturn(errandEntity);

		// Act
		service.deleteErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID);

		// Assert
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW);
		verify(entityManagerMock).lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		verify(errandsRepositoryMock).save(errandEntity);
		assertThat(errandEntity.getMeasures()).isEmpty();
	}

	@Test
	void deleteErrandMeasureNotFound() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(List.of());

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);

		// Act & Assert
		assertThatThrownBy(() -> service.deleteErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("could not be found");
	}
}
