package se.sundsvall.supportmanagement.service.job;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;
import se.sundsvall.supportmanagement.config.LabelRestructureProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.MetadataService;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.ADD;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.DELETE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MERGE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MOVE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.RENAME;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;

/**
 * The orchestration itself is what these tests pin down: which {@code MetadataService} mutation primitive each step
 * type dispatches to, that the restow pass runs once over the whole frozen {@code errandIds} set rather than once
 * per step, that a MERGE step's sources are substituted during that restow and deleted only afterward, and that any
 * failure - a step throwing, a stop request, a lock-acquisition timeout - rolls back without completing the job or
 * emitting an audit event. Whether a given mutation itself is valid is {@code MetadataService}'s own concern, tested
 * there - every mutation call here is a plain mock.
 */
@ExtendWith(MockitoExtension.class)
class LabelRestructureRunnerTest {

	private static final String JOB_ID = "job-id";
	private static final String NAMESPACE = "NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String STARTED_BY = "joe01doe; type=AD_ACCOUNT";
	private static final int BATCH_SIZE = 2;
	private static final Duration PROGRESS_INTERVAL = Duration.ofMinutes(1);

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private MetadataService metadataServiceMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private EventService eventServiceMock;

	private LabelRestructureRunner runner;

	@BeforeEach
	void createRunner() {
		runner = new LabelRestructureRunner(metadataLabelRepositoryMock, errandsRepositoryMock, errandServiceMock, metadataServiceMock, jobServiceMock, eventServiceMock,
			new LabelRestructureProperties(BATCH_SIZE, 2, PROGRESS_INTERVAL, Duration.ofSeconds(20), 10000),
			mock(LabelRestructureRunner.class));
		// Self-invokes through this field (see the class's own javadoc) rather than through 'this', so that a plain
		// constructor call here - which cannot resolve a mock of the very class under construction - would otherwise
		// leave it pointing at a bare mock that never carries out executeRestructure for real.
		ReflectionTestUtils.setField(runner, "self", runner);
	}

	@Test
	@DisplayName("Verification that each step type dispatches to its own MetadataService mutation primitive, the restow pass runs once over the whole errandIds set, a MERGE step's sources are substituted during that restow and deleted only afterward, and the job completes with audit events for the MOVE/MERGE steps only")
	void run_addRenameDeleteMoveMerge_appliesEachStepAndCompletesJobWithAuditEventsForMoveAndMergeOnly() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/OLD")).thenReturn(Optional.of(label("old-id")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY2")).thenReturn(Optional.of(label("cat2-id")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/TARGET")).thenReturn(Optional.of(label("target-id")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/SOURCE")).thenReturn(Optional.of(label("source-id")));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));
		final var errand1 = errand("errand-1");
		final var errand2 = errand("errand-2");
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1", "errand-2"))).thenReturn(List.of(errand1, errand2));
		when(errandServiceMock.persistLabelRestructureMigration(any(), any(), anyBoolean())).thenReturn(true);

		final var steps = List.of(
			addStep(List.of("CATEGORY", "NEW"), "New type", "TYPE"),
			renameStep(List.of("CATEGORY", "RENAME_ME"), "Renamed"),
			deleteStep(List.of("CATEGORY", "DELETE_ME")),
			moveStep(List.of("CATEGORY", "OLD"), List.of("CATEGORY2")),
			mergeStep(List.of("CATEGORY", "TARGET"), List.of(List.of("CATEGORY", "SOURCE"))));

		runner.run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, List.of("errand-1", "errand-2"), STARTED_BY, true));

		verify(metadataServiceMock).addLabel(NAMESPACE, MUNICIPALITY_ID, List.of("CATEGORY", "NEW"), "New type", "TYPE");
		verify(metadataServiceMock).renameLabel(NAMESPACE, MUNICIPALITY_ID, List.of("CATEGORY", "RENAME_ME"), "Renamed");
		verify(metadataServiceMock).deleteLabel(NAMESPACE, MUNICIPALITY_ID, List.of("CATEGORY", "DELETE_ME"));
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, "old-id", "cat2-id", null, null);
		verify(metadataServiceMock).mergeInto(NAMESPACE, MUNICIPALITY_ID, "target-id", Set.of("source-id"));

		final var substitutions = Map.of("source-id", "target-id");
		verify(errandServiceMock).persistLabelRestructureMigration(errand1, substitutions, true);
		verify(errandServiceMock).persistLabelRestructureMigration(errand2, substitutions, true);
		verify(errandsRepositoryMock).existsByLabelsMetadataLabelIdIn(Set.of("source-id"));

		// Deleted only after the restow above, not alongside mergeInto - the restow still needs to read the source's
		// own tree position.
		verify(metadataServiceMock).deleteMergedLabels(Set.of("source-id"));

		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq("old-id"), eq(STARTED_BY), anyString());
		verify(eventServiceMock).createLabelMergeEvent(eq(MUNICIPALITY_ID), eq("target-id"), eq(STARTED_BY), anyString());
		verifyNoMoreInteractions(eventServiceMock);

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(jobServiceMock).complete(JOB_ID, "5 step(s) applied, 2 errand(s) restowed");
		verify(jobServiceMock, never()).fail(any(), any());
	}

	@Test
	@DisplayName("Verification that a run with no MOVE/MERGE steps skips the restow pass and deleteMergedLabels entirely, but still checks whether the job was stopped before completing it")
	void run_noMoveOrMergeSteps_skipsRestowAndDeleteMergedLabels() {
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));
		final var steps = List.of(addStep(List.of("CATEGORY"), "Category", "CATEGORY"));

		runner.run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, emptyList(), STARTED_BY, true));

		verify(metadataServiceMock).addLabel(NAMESPACE, MUNICIPALITY_ID, List.of("CATEGORY"), "Category", "CATEGORY");
		verify(errandsRepositoryMock, never()).findAllByIdForUpdate(any());
		verify(metadataServiceMock, never()).deleteMergedLabels(any());
		verifyNoInteractions(eventServiceMock);
		verify(jobServiceMock).complete(JOB_ID, "1 step(s) applied, 0 errand(s) restowed");
	}

	@Test
	@DisplayName("Verification that a step throwing fails the job with a message naming which step failed, without reaching the restow pass or emitting an audit event for any step, named or not")
	void run_stepFails_failsJobNamingTheStepWithoutRestowingOrAuditing() {
		metadataServiceThrowsOnRename("Label at path 'CATEGORY/RENAME_ME' no longer exists");

		final var steps = List.of(
			addStep(List.of("CATEGORY"), "Category", "CATEGORY"),
			renameStep(List.of("CATEGORY", "RENAME_ME"), "Renamed"));

		runner.run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, List.of("errand-1"), STARTED_BY, true));

		verify(metadataServiceMock).addLabel(NAMESPACE, MUNICIPALITY_ID, List.of("CATEGORY"), "Category", "CATEGORY");
		verify(errandsRepositoryMock, never()).findAllByIdForUpdate(any());
		verifyNoInteractions(eventServiceMock);
		verify(jobServiceMock, never()).complete(any(), any());

		final var messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(jobServiceMock).fail(eq(JOB_ID), messageCaptor.capture());
		assertThat(messageCaptor.getValue())
			.contains("Reached 0 of 1 errand(s) before")
			.contains("step 1 (RENAME) failed")
			.contains("Label at path 'CATEGORY/RENAME_ME' no longer exists");
	}

	@Test
	@DisplayName("Verification that a job no longer RUNNING when a chunk boundary is reached rolls the whole run back, reports progress up to that point, and fails the job without completing or auditing it")
	void run_jobNoLongerRunning_rollsBackReportsProgressAndFailsWithoutCompletingOrAuditing() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY2")).thenReturn(Optional.of(label("cat2-id")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/OLD")).thenReturn(Optional.of(label("old-id")));
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1", "errand-2"))).thenReturn(List.of(errand("errand-1"), errand("errand-2")));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(STOPPED));

		final var steps = List.of(moveStep(List.of("CATEGORY", "OLD"), List.of("CATEGORY2")));

		runner.run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, List.of("errand-1", "errand-2"), STARTED_BY, true));

		verify(errandServiceMock, times(2)).persistLabelRestructureMigration(any(), any(), anyBoolean());
		// Reported twice with the same count: once at the chunk boundary before the stop check fires, and again from
		// the catch block that reports the exact point of failure before wrapping and rethrowing.
		verify(jobServiceMock, times(2)).updateProgress(JOB_ID, 2);
		verify(jobServiceMock, never()).complete(any(), any());
		verifyNoInteractions(eventServiceMock);

		final var messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(jobServiceMock).fail(eq(JOB_ID), messageCaptor.capture());
		assertThat(messageCaptor.getValue())
			.contains("Reached 2 of 2 errand(s) before")
			.contains("stopped by request");
	}

	@Test
	@DisplayName("Verification that affected errands are restowed in chunks no larger than the configured batch size, and that progress is reported at every chunk boundary")
	void run_multipleChunks_locksAndRestowsEachChunkOnItsOwnAndReportsProgressPerChunk() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY2")).thenReturn(Optional.of(label("cat2-id")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/OLD")).thenReturn(Optional.of(label("old-id")));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1", "errand-2"))).thenReturn(List.of(errand("errand-1"), errand("errand-2")));
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-3"))).thenReturn(List.of(errand("errand-3")));
		when(errandServiceMock.persistLabelRestructureMigration(any(), any(), anyBoolean())).thenReturn(true);

		final var steps = List.of(moveStep(List.of("CATEGORY", "OLD"), List.of("CATEGORY2")));

		runner.run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, List.of("errand-1", "errand-2", "errand-3"), STARTED_BY, true));

		verify(errandsRepositoryMock).findAllByIdForUpdate(List.of("errand-1", "errand-2"));
		verify(errandsRepositoryMock).findAllByIdForUpdate(List.of("errand-3"));
		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).updateProgress(JOB_ID, 3);
		verify(jobServiceMock).complete(JOB_ID, "1 step(s) applied, 3 errand(s) restowed");
	}

	@Test
	@DisplayName("Verification that a lock-acquisition timeout fails the job with a friendly, operator-facing reason rather than the bare exception message, without completing or auditing it")
	void run_lockAcquisitionTimesOut_failsJobWithFriendlyMessageWithoutCompletingOrAuditing() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY2")).thenReturn(Optional.of(label("cat2-id")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/OLD")).thenReturn(Optional.of(label("old-id")));
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1"))).thenThrow(new CannotAcquireLockException("lock wait timeout"));

		final var steps = List.of(moveStep(List.of("CATEGORY", "OLD"), List.of("CATEGORY2")));

		runner.run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, List.of("errand-1"), STARTED_BY, true));

		verify(jobServiceMock, never()).complete(any(), any());
		verifyNoInteractions(eventServiceMock);

		final var messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(jobServiceMock).fail(eq(JOB_ID), messageCaptor.capture());
		assertThat(messageCaptor.getValue()).contains("could not acquire locks - one or more errands are being edited; retry when the namespace is idle");
	}

	@Test
	@DisplayName("Verification that an errand whose rebuilt labels a guard refuses is counted apart from those restowed, and that the run is told whether it was started by an ad account")
	void run_guardRefusesAnErrand_countsItApartFromThoseRestowed() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY2")).thenReturn(Optional.of(label("cat2-id")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/OLD")).thenReturn(Optional.of(label("old-id")));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));
		final var errand1 = errand("errand-1");
		final var errand2 = errand("errand-2");
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1", "errand-2"))).thenReturn(List.of(errand1, errand2));
		when(errandServiceMock.persistLabelRestructureMigration(errand1, Map.of(), false)).thenReturn(true);
		when(errandServiceMock.persistLabelRestructureMigration(errand2, Map.of(), false)).thenReturn(false);

		final var steps = List.of(moveStep(List.of("CATEGORY", "OLD"), List.of("CATEGORY2")));

		runner.run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, List.of("errand-1", "errand-2"), STARTED_BY, false));

		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).complete(JOB_ID, "1 step(s) applied, 1 errand(s) restowed, 1 kept their labels");
		verify(jobServiceMock, never()).fail(any(), any());
	}

	@Test
	@DisplayName("Verification that a MERGE step whose source an errand still wears after the restow - kept on it by a guard - rolls the run back without deleting the source, completing or auditing it")
	void run_mergeSourceStillWornAfterRestow_failsWithoutDeletingTheSource() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/TARGET")).thenReturn(Optional.of(label("target-id")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/SOURCE")).thenReturn(Optional.of(label("source-id")));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));
		final var errand1 = errand("errand-1");
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1"))).thenReturn(List.of(errand1));
		when(errandServiceMock.persistLabelRestructureMigration(errand1, Map.of("source-id", "target-id"), true)).thenReturn(false);
		when(errandsRepositoryMock.existsByLabelsMetadataLabelIdIn(Set.of("source-id"))).thenReturn(true);

		final var steps = List.of(mergeStep(List.of("CATEGORY", "TARGET"), List.of(List.of("CATEGORY", "SOURCE"))));

		runner.run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, List.of("errand-1"), STARTED_BY, true));

		verify(metadataServiceMock).mergeInto(NAMESPACE, MUNICIPALITY_ID, "target-id", Set.of("source-id"));
		verify(metadataServiceMock, never()).deleteMergedLabels(any());
		verify(jobServiceMock, never()).complete(any(), any());
		verifyNoInteractions(eventServiceMock);

		final var messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(jobServiceMock).fail(eq(JOB_ID), messageCaptor.capture());
		assertThat(messageCaptor.getValue())
			.contains("Reached 1 of 1 errand(s) before")
			.contains("labels [source-id] were merged away but errands still wear them");
	}

	private void metadataServiceThrowsOnRename(final String message) {
		doThrow(new IllegalStateException(message)).when(metadataServiceMock)
			.renameLabel(NAMESPACE, MUNICIPALITY_ID, List.of("CATEGORY", "RENAME_ME"), "Renamed");
	}

	private static MetadataLabelEntity label(final String id) {
		return MetadataLabelEntity.create().withId(id);
	}

	private static ErrandEntity errand(final String id) {
		return ErrandEntity.create().withId(id);
	}

	private static LabelRestructureStep addStep(final List<String> path, final String displayName, final String classification) {
		return LabelRestructureStep.create().withType(ADD).withPath(path).withDisplayName(displayName).withClassification(classification);
	}

	private static LabelRestructureStep renameStep(final List<String> path, final String displayName) {
		return LabelRestructureStep.create().withType(RENAME).withPath(path).withDisplayName(displayName);
	}

	private static LabelRestructureStep deleteStep(final List<String> path) {
		return LabelRestructureStep.create().withType(DELETE).withPath(path);
	}

	private static LabelRestructureStep moveStep(final List<String> path, final List<String> destinationParentPath) {
		return LabelRestructureStep.create().withType(MOVE).withPath(path).withDestinationParentPath(destinationParentPath);
	}

	private static LabelRestructureStep mergeStep(final List<String> path, final List<List<String>> sourcePaths) {
		return LabelRestructureStep.create().withType(MERGE).withPath(path).withSourcePaths(sourcePaths);
	}
}
