package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasure;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasureEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasures;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.updateMeasureEntity;

class ErrandMeasureMapperTest {

	@Test
	void testToMeasureEntity() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId("errand-id");
		final var measure = new Measure()
			.withResponsibleUser("jo12doe")
			.withType("INTERVENTION")
			.withPlannedStart(OffsetDateTime.now())
			.withPlannedComplete(OffsetDateTime.now().plusDays(30))
			.withExecuted(OffsetDateTime.now().plusDays(15))
			.withAddedByUser("admin")
			.withAddedByRole("MANAGER")
			.withGoal("Improve response time")
			.withDescription("Detailed description")
			.withAccept("TRUE")
			.withAcceptMotivation("Approved")
			.withReworkGoal("Rework goal")
			.withReworkDescription("Rework description");

		// Act
		final var result = toMeasureEntity(measure, errandEntity);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(result.getResponsibleUser()).isEqualTo("jo12doe");
		assertThat(result.getType()).isEqualTo("INTERVENTION");
		assertThat(result.getPlannedStart()).isEqualTo(measure.getPlannedStart());
		assertThat(result.getPlannedComplete()).isEqualTo(measure.getPlannedComplete());
		assertThat(result.getExecuted()).isEqualTo(measure.getExecuted());
		assertThat(result.getAddedByUser()).isEqualTo("admin");
		assertThat(result.getAddedByRole()).isEqualTo("MANAGER");
		assertThat(result.getGoal()).isEqualTo("Improve response time");
		assertThat(result.getDescription()).isEqualTo("Detailed description");
		assertThat(result.getAccept()).isEqualTo(Accept.TRUE);
		assertThat(result.getAcceptMotivation()).isEqualTo("Approved");
		assertThat(result.getReworkGoal()).isEqualTo("Rework goal");
		assertThat(result.getReworkDescription()).isEqualTo("Rework description");
	}

	@Test
	void testToMeasureEntityWithNullAccept() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId("errand-id");
		final var measure = new Measure().withType("INTERVENTION");

		// Act
		final var result = toMeasureEntity(measure, errandEntity);

		// Assert
		assertThat(result.getAccept()).isNull();
	}

	@Test
	void testUpdateMeasureEntity() {

		// Arrange
		final var entity = MeasureEntity.create()
			.withId("id")
			.withType("OLD_TYPE")
			.withGoal("old goal")
			.withDescription("old description");

		final var measure = new Measure()
			.withGoal("new goal")
			.withAccept("REWORK");

		// Act
		final var result = updateMeasureEntity(entity, measure);

		// Assert
		assertThat(result).isSameAs(entity);
		assertThat(result.getType()).isEqualTo("OLD_TYPE");
		assertThat(result.getGoal()).isEqualTo("new goal");
		assertThat(result.getDescription()).isEqualTo("old description");
		assertThat(result.getAccept()).isEqualTo(Accept.REWORK);
	}

	@Test
	void testUpdateMeasureEntityAllNulls() {

		// Arrange
		final var entity = MeasureEntity.create()
			.withId("id")
			.withType("TYPE")
			.withGoal("goal");

		final var measure = new Measure();

		// Act
		final var result = updateMeasureEntity(entity, measure);

		// Assert
		assertThat(result.getType()).isEqualTo("TYPE");
		assertThat(result.getGoal()).isEqualTo("goal");
	}

	@Test
	void testToMeasure() {

		// Arrange
		final var now = OffsetDateTime.now();
		final var entity = MeasureEntity.create()
			.withId("measure-id")
			.withResponsibleUser("jo12doe")
			.withType("INTERVENTION")
			.withPlannedStart(now)
			.withPlannedComplete(now.plusDays(30))
			.withExecuted(now.plusDays(15))
			.withAddedByUser("admin")
			.withAddedByRole("MANAGER")
			.withGoal("goal")
			.withDescription("description")
			.withAccept(Accept.FALSE)
			.withAcceptMotivation("motivation")
			.withReworkGoal("rework goal")
			.withReworkDescription("rework description")
			.withCreated(now)
			.withModified(now.plusHours(1));

		// Act
		final var result = toMeasure(entity);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("measure-id");
		assertThat(result.getResponsibleUser()).isEqualTo("jo12doe");
		assertThat(result.getType()).isEqualTo("INTERVENTION");
		assertThat(result.getPlannedStart()).isEqualTo(now);
		assertThat(result.getPlannedComplete()).isEqualTo(now.plusDays(30));
		assertThat(result.getExecuted()).isEqualTo(now.plusDays(15));
		assertThat(result.getAddedByUser()).isEqualTo("admin");
		assertThat(result.getAddedByRole()).isEqualTo("MANAGER");
		assertThat(result.getGoal()).isEqualTo("goal");
		assertThat(result.getDescription()).isEqualTo("description");
		assertThat(result.getAccept()).isEqualTo("FALSE");
		assertThat(result.getAcceptMotivation()).isEqualTo("motivation");
		assertThat(result.getReworkGoal()).isEqualTo("rework goal");
		assertThat(result.getReworkDescription()).isEqualTo("rework description");
		assertThat(result.getCreated()).isEqualTo(now);
		assertThat(result.getModified()).isEqualTo(now.plusHours(1));
	}

	@Test
	void testToMeasureWithNullAccept() {

		// Arrange
		final var entity = MeasureEntity.create().withId("id").withType("TYPE");

		// Act
		final var result = toMeasure(entity);

		// Assert
		assertThat(result.getAccept()).isNull();
	}

	@Test
	void testToMeasures() {

		// Arrange
		final var entities = List.of(
			MeasureEntity.create().withId("id-1").withType("TYPE_1"),
			MeasureEntity.create().withId("id-2").withType("TYPE_2"));

		// Act
		final var result = toMeasures(entities);

		// Assert
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo("id-1");
		assertThat(result.get(1).getId()).isEqualTo("id-2");
	}

	@Test
	void testToMeasuresWithNull() {

		// Act
		final var result = toMeasures(null);

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void testMergeMeasuresKeepsIdAndUpdatesInPlace() {

		// Arrange
		final var created = OffsetDateTime.now().minusDays(3);
		final var existing = MeasureEntity.create()
			.withId("measure-id")
			.withGoal("Original goal")
			.withDescription("Original description")
			.withCreated(created);
		final var errandEntity = ErrandEntity.create().withId("errand-id").withMeasures(new ArrayList<>(List.of(existing)));

		// Act - the round trip a client makes, reading the errand and patching it back with one field changed
		ErrandMeasureMapper.mergeMeasures(errandEntity, List.of(new Measure().withId("measure-id").withGoal("Changed goal")));

		// Assert - same row, same id, same created, so every Location previously handed out still resolves
		assertThat(errandEntity.getMeasures()).containsExactly(existing);
		assertThat(existing.getId()).isEqualTo("measure-id");
		assertThat(existing.getCreated()).isEqualTo(created);
		assertThat(existing.getGoal()).isEqualTo("Changed goal");
		assertThat(existing.getDescription()).isEqualTo("Original description");
	}

	@Test
	void testMergeMeasuresRemovesOmittedOnes() {

		// Arrange
		final var retained = MeasureEntity.create().withId("retained-id");
		final var omitted = MeasureEntity.create().withId("omitted-id");
		final var errandEntity = ErrandEntity.create().withMeasures(new ArrayList<>(List.of(retained, omitted)));

		// Act
		ErrandMeasureMapper.mergeMeasures(errandEntity, List.of(new Measure().withId("retained-id")));

		// Assert
		assertThat(errandEntity.getMeasures()).containsExactly(retained);
	}

	@Test
	void testMergeMeasuresAddsMeasureWithoutId() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withMeasures(new ArrayList<>());

		// Act
		ErrandMeasureMapper.mergeMeasures(errandEntity, List.of(new Measure().withGoal("New goal")));

		// Assert - the id is left for the generator to assign on persist
		assertThat(errandEntity.getMeasures()).hasSize(1);
		assertThat(errandEntity.getMeasures().getFirst().getId()).isNull();
		assertThat(errandEntity.getMeasures().getFirst().getGoal()).isEqualTo("New goal");
		assertThat(errandEntity.getMeasures().getFirst().getErrandEntity()).isEqualTo(errandEntity);
	}

	@Test
	void testMergeMeasuresTreatsForeignIdAsNew() {

		// Arrange - an id belonging to another errand must never reach across to this one
		final var errandEntity = ErrandEntity.create().withMeasures(new ArrayList<>());

		// Act
		ErrandMeasureMapper.mergeMeasures(errandEntity, List.of(new Measure().withId("id-of-another-errands-measure").withGoal("New goal")));

		// Assert
		assertThat(errandEntity.getMeasures()).hasSize(1);
		assertThat(errandEntity.getMeasures().getFirst().getId()).isNull();
		assertThat(errandEntity.getMeasures().getFirst().getGoal()).isEqualTo("New goal");
	}

	@Test
	void testMergeMeasuresHandlesNullList() {

		// Arrange
		final var existing = MeasureEntity.create().withId("measure-id");
		final var errandEntity = ErrandEntity.create().withMeasures(new ArrayList<>(List.of(existing)));

		// Act
		ErrandMeasureMapper.mergeMeasures(errandEntity, null);

		// Assert
		assertThat(errandEntity.getMeasures()).isEmpty();
	}
}
