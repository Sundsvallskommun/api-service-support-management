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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class ErrandMeasureServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errand-id";
	private static final String MEASURE_ID = "measure-id";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MeasureValidator measureValidatorMock;

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

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);
		when(errandsRepositoryMock.saveAndFlush(any())).thenReturn(errandEntity);

		// Act
		service.createErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, measure);

		// Assert
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW);
		verify(entityManagerMock).lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		verify(entityManagerMock).persist(errandEntity.getMeasures().getFirst());
		verify(errandsRepositoryMock).saveAndFlush(errandEntity);
		assertThat(errandEntity.getMeasures()).hasSize(1);
		assertThat(errandEntity.getMeasures().getFirst().getType()).isEqualTo("INTERVENTION");
		assertThat(errandEntity.getMeasures().getFirst().getGoal()).isEqualTo("goal");
	}

	/**
	 * What the validator accepts is MeasureValidatorTest's business. What matters here is that it is consulted, and that
	 * its rejection prevents the measure from being saved.
	 */
	@Test
	void createErrandMeasureRejectedByValidator() {

		// Arrange
		final var measure = new Measure().withType("INVALID_TYPE").withAddedByRole("MANAGER");

		final var measureEntity = MeasureEntity.create().withId(MEASURE_ID);
		final var errandEntity = ErrandEntity.create().withMeasures(new ArrayList<>(List.of(measureEntity)));
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW)).thenReturn(errandEntity);

		doThrow(Problem.valueOf(BAD_REQUEST, "'INVALID_TYPE' is not a valid measure type")).when(measureValidatorMock).validate(measure, NAMESPACE, MUNICIPALITY_ID);

		// Act & Assert
		assertThatThrownBy(() -> service.createErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, measure))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("not a valid measure type");

		verify(measureValidatorMock).validate(measure, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandsRepositoryMock);
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
		when(errandsRepositoryMock.saveAndFlush(any())).thenReturn(errandEntity);

		// Act
		final var result = service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, "\"0\"", measure);

		// Assert
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW);
		verify(entityManagerMock).lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		verify(errandsRepositoryMock).saveAndFlush(errandEntity);
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

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);
		when(errandsRepositoryMock.saveAndFlush(any())).thenReturn(errandEntity);

		// Act
		final var result = service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, "\"0\"", measure);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getType()).isEqualTo("SUPPORT");
	}

	@Test
	void updateErrandMeasureRejectedByValidator() {

		// Arrange
		final var measure = new Measure().withType("INVALID_TYPE");

		final var measureEntity = MeasureEntity.create().withId(MEASURE_ID);
		final var errandEntity = ErrandEntity.create().withMeasures(new ArrayList<>(List.of(measureEntity)));
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW)).thenReturn(errandEntity);

		doThrow(Problem.valueOf(BAD_REQUEST, "'INVALID_TYPE' is not a valid measure type")).when(measureValidatorMock).validate(measure, measureEntity, NAMESPACE, MUNICIPALITY_ID);

		// Act & Assert
		assertThatThrownBy(() -> service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, "\"0\"", measure))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("not a valid measure type");

		verify(measureValidatorMock).validate(measure, measureEntity, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(errandsRepositoryMock);
	}

	@Test
	void updateErrandMeasureNotFound() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(List.of());

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(errandEntity);

		// Act & Assert
		assertThatThrownBy(() -> service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, "\"0\"", new Measure()))
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
		service.deleteErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, "\"0\"");

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
		assertThatThrownBy(() -> service.deleteErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, "\"0\""))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("could not be found");
	}

	@Test
	void staleUpdateDoesNotChangeTheMeasure() {
		final var measure = MeasureEntity.create().withId(MEASURE_ID).withVersion(3L).withGoal("Current goal");
		final var errand = ErrandEntity.create().withVersion(2L).withMeasures(List.of(measure));
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW)).thenReturn(errand);
		assertThatThrownBy(() -> service.updateErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, "\"2\"", Measure.create().withGoal("Stale goal")))
			.hasMessageContaining("Precondition Failed");
		assertThat(measure.getGoal()).isEqualTo("Current goal");
		verifyNoInteractions(errandsRepositoryMock, measureValidatorMock);
	}

	@Test
	void staleDeleteDoesNotRemoveTheMeasure() {
		final var measure = MeasureEntity.create().withId(MEASURE_ID).withVersion(3L);
		final var errand = ErrandEntity.create().withVersion(2L).withMeasures(List.of(measure));
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.MEASURE, RW)).thenReturn(errand);
		assertThatThrownBy(() -> service.deleteErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, "\"2\""))
			.hasMessageContaining("Precondition Failed");
		assertThat(errand.getMeasures()).containsExactly(measure);
		verifyNoInteractions(errandsRepositoryMock);
	}
}
