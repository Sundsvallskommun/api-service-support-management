package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.LabelAttributeEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_BLOCKED_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_KEY_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_START_MODE_ATTRIBUTE;

@ExtendWith(MockitoExtension.class)
class ProcessKeySelectorTest {

	private static final String APPLICATION = "alkt-ansokan";
	private static final String SUPERVISION = "alkt-tillsyn";

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@InjectMocks
	private ProcessKeySelector selector;

	private static Stream<Arguments> unreadableStartModes() {
		return Stream.of(
			arguments("SOMETHING_ELSE"),
			arguments("automatiskt"),
			arguments("0"));
	}

	@Test
	void oneLabelWithAKeyResolvesToThatKey() {
		final var selection = selector.select(errandWith(label(APPLICATION, null)));

		assertThat(selection.processKey()).isEqualTo(APPLICATION);
		assertThat(selection.keys()).containsExactly(APPLICATION);
		assertThat(selection.isAmbiguous()).isFalse();
	}

	@Test
	@DisplayName("Verification that two labels naming the same process are one process rather than an ambiguity")
	void twoLabelsCarryingTheSameKeyResolveToOne() {
		final var selection = selector.select(errandWith(label(APPLICATION, null), label(APPLICATION, null)));

		assertThat(selection.processKey()).isEqualTo(APPLICATION);
		assertThat(selection.keys()).containsExactly(APPLICATION);
		assertThat(selection.isAmbiguous()).isFalse();
	}

	@Test
	@DisplayName("Verification that two labels pointing in different directions resolve to nothing, and name both keys")
	void twoLabelsCarryingDifferentKeysAreAmbiguous() {
		final var selection = selector.select(errandWith(label(SUPERVISION, null), label(APPLICATION, null)));

		assertThat(selection.processKey()).isNull();
		assertThat(selection.startMode()).isNull();
		assertThat(selection.isAmbiguous()).isTrue();
		assertThat(selection.keys()).containsExactly(APPLICATION, SUPERVISION);
	}

	@Test
	void aDeprecatedLabelIsNotRead() {
		final var deprecated = label(SUPERVISION, null).withDeprecated(true);

		final var selection = selector.select(errandWith(label(APPLICATION, null), deprecated));

		assertThat(selection.processKey()).isEqualTo(APPLICATION);
		assertThat(selection.keys()).containsExactly(APPLICATION);
	}

	@Test
	@DisplayName("Verification that an errand wearing only deprecated labels runs no process, which is not an error")
	void onlyDeprecatedLabelsResolveToNothing() {
		final var selection = selector.select(errandWith(label(APPLICATION, null).withDeprecated(true)));

		assertThat(selection.processKey()).isNull();
		assertThat(selection.keys()).isEmpty();
		assertThat(selection.isAmbiguous()).isFalse();
	}

	@Test
	@DisplayName("Verification that the resolution hangs on the attribute, so renaming the label or moving it in the tree changes nothing")
	void renamingOrMovingTheLabelLeavesTheResolutionAlone() {
		final var before = label(APPLICATION, null)
			.withResourceName("CATEGORY-1")
			.withResourcePath("CATEGORY-1");

		final var renamedAndMoved = label(APPLICATION, null)
			.withResourceName("SOMETHING-ELSE")
			.withResourcePath("ROOT/LEVEL-ONE/SOMETHING-ELSE")
			.withParent(MetadataLabelEntity.create().withId(randomUUID().toString()));

		assertThat(selector.select(errandWith(renamedAndMoved))).isEqualTo(selector.select(errandWith(before)));
	}

	@Test
	void aLabelWithoutAProcessKeyIsNotRead() {
		final var selection = selector.select(errandWith(MetadataLabelEntity.create()
			.withId(randomUUID().toString())
			.withAttributes(List.of(attribute("escalationEmail", "escalation@example.com")))));

		assertThat(selection.processKey()).isNull();
		assertThat(selection.keys()).isEmpty();
	}

	@Test
	void aBlankProcessKeyIsNotRead() {
		final var selection = selector.select(errandWith(label("   ", null)));

		assertThat(selection.processKey()).isNull();
		assertThat(selection.keys()).isEmpty();
	}

	@Test
	void anErrandWithoutLabelsResolvesToNothing() {
		assertThat(selector.select(ErrandEntity.create())).isEqualTo(ProcessKeySelection.NONE);
	}

	@Test
	@DisplayName("Verification that a label the errand has only just been given is looked up by id, since it points at nothing until the errand is loaded, and an errand created wearing it would otherwise start no process")
	void aLabelNotYetLoadedIsLookedUpById() {
		final var application = label(APPLICATION, "MANUAL");
		when(metadataLabelRepositoryMock.findAllById(Set.of(application.getId()))).thenReturn(List.of(application));

		final var selection = selector.select(errandWearing(notLoaded(application.getId())));

		assertThat(selection.processKey()).isEqualTo(APPLICATION);
		assertThat(selection.startMode()).isEqualTo(MANUAL);
	}

	@Test
	@DisplayName("Verification that the labels of an errand read from the database are taken as they stand, so the common case costs no query")
	void theLabelsOfALoadedErrandAreReadWithoutALookup() {
		assertThat(selector.select(errandWith(label(APPLICATION, null))).processKey()).isEqualTo(APPLICATION);

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that the labels an errand wears and the ones it has just been given are read together, the new ones in one lookup, so that a second process among them is the ambiguity it is")
	void loadedAndNewlyGivenLabelsAreReadTogether() {
		final var supervision = label(SUPERVISION, null);
		final var reference = MetadataLabelEntity.create()
			.withId(randomUUID().toString())
			.withAttributes(List.of(attribute("escalationEmail", "escalation@example.com")));
		when(metadataLabelRepositoryMock.findAllById(Set.of(supervision.getId(), reference.getId()))).thenReturn(List.of(supervision, reference));

		final var selection = selector.select(errandWearing(loaded(label(APPLICATION, null)), notLoaded(supervision.getId()), notLoaded(reference.getId())));

		assertThat(selection.isAmbiguous()).isTrue();
		assertThat(selection.keys()).containsExactly(APPLICATION, SUPERVISION);
		verify(metadataLabelRepositoryMock).findAllById(Set.of(supervision.getId(), reference.getId()));
	}

	@Test
	@DisplayName("Verification that a label the lookup cannot find is passed over rather than thrown on, as one that is gone always has been")
	void aLabelThatCannotBeFoundIsPassedOver() {
		final var missing = randomUUID().toString();
		when(metadataLabelRepositoryMock.findAllById(Set.of(missing))).thenReturn(List.of());

		assertThat(selector.select(errandWearing(notLoaded(missing))).keys()).isEmpty();
	}

	@Test
	@DisplayName("Verification that an entry naming no label at all is passed over without a lookup")
	void anEntryNamingNoLabelIsPassedOverWithoutALookup() {
		assertThat(selector.select(errandWearing(ErrandLabelEmbeddable.create()))).isEqualTo(ProcessKeySelection.NONE);

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a label saying nothing about the start mode behaves as it did before the attribute existed")
	void aLabelWithoutAStartModeIsAutomatic() {
		assertThat(selector.select(errandWith(label(APPLICATION, null))).startMode()).isEqualTo(AUTOMATIC);
	}

	@Test
	@DisplayName("Verification that the start mode is read from the very label that gave the key")
	void theStartModeIsReadFromTheLabelThatGaveTheKey() {
		assertThat(selector.select(errandWith(label(SUPERVISION, "MANUAL"))).startMode()).isEqualTo(MANUAL);
	}

	@Test
	void theStartModeIsReadWithoutRegardToCase() {
		assertThat(selector.select(errandWith(label(SUPERVISION, "manual"))).startMode()).isEqualTo(MANUAL);
	}

	@ParameterizedTest
	@MethodSource("unreadableStartModes")
	@DisplayName("Verification that a start mode this cannot read stops an automatic start rather than causing one")
	void anUnreadableStartModeIsManual(final String value) {
		assertThat(selector.select(errandWith(label(APPLICATION, value))).startMode()).isEqualTo(MANUAL);
	}

	@Test
	@DisplayName("Verification that two labels naming different processes with different start modes give no start mode at all, in either order")
	void labelsNamingDifferentProcessesGiveNoStartMode() {
		assertThat(selector.select(errandWith(label(APPLICATION, "MANUAL"), label(SUPERVISION, "AUTOMATIC"))))
			.isEqualTo(new ProcessKeySelection(null, null, List.of(APPLICATION, SUPERVISION)));
		assertThat(selector.select(errandWith(label(SUPERVISION, "AUTOMATIC"), label(APPLICATION, "MANUAL"))))
			.isEqualTo(new ProcessKeySelection(null, null, List.of(APPLICATION, SUPERVISION)));
	}

	@Test
	@DisplayName("Verification that a MANUAL on one of two labels naming the same process wins over the other label's silence")
	void aManualAmongLabelsSharingAKeyWins() {
		final var selection = selector.select(errandWith(label(APPLICATION, null), label(APPLICATION, "MANUAL")));

		assertThat(selection.processKey()).isEqualTo(APPLICATION);
		assertThat(selection.startMode()).isEqualTo(MANUAL);
	}

	@Test
	void aKeyThatFitsIsNamedAsItIs() {
		assertThat(ProcessKeySelector.excerptOf(APPLICATION)).isEqualTo(APPLICATION);
	}

	@Test
	@DisplayName("Verification that a key too long to name in full is cut, so that a message about it is not made of it")
	void aKeyTooLongToNameIsCut() {
		final var excerpt = ProcessKeySelector.excerptOf("k".repeat(1000));

		assertThat(excerpt).hasSize(64).endsWith("...");
	}

	@Test
	void theKeysOfAnAmbiguousErrandAreNamedTogether() {
		assertThat(ProcessKeySelector.excerptOf(List.of(APPLICATION, SUPERVISION))).isEqualTo(APPLICATION + ", " + SUPERVISION);
	}

	@Test
	@DisplayName("Verification that each key of an ambiguous errand is cut on its own, rather than the list as a whole")
	void eachKeyOfAnAmbiguousErrandIsCutOnItsOwn() {
		final var excerpt = ProcessKeySelector.excerptOf(List.of("k".repeat(1000), APPLICATION));

		assertThat(excerpt).endsWith(", " + APPLICATION).hasSize(64 + ", ".length() + APPLICATION.length());
	}

	@Test
	@DisplayName("Verification that an errand wearing a label with processBlocked=true among others is blocked, and that the ids of only the blocking labels are named")
	void aLabelWithProcessBlockedTrueBlocks() {
		final var blocking = blocking("true");
		final var application = label(APPLICATION, null);

		assertThat(selector.isBlocked(errandWith(application, blocking))).isTrue();
		assertThat(selector.blockingLabelIdsOf(List.of(loaded(application), loaded(blocking)))).containsExactly(blocking.getId());
		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a blocking label the errand has just been given is looked up by id and blocks")
	void aNewlyGivenBlockingLabelIsLookedUp() {
		final var blocking = blocking("true");
		when(metadataLabelRepositoryMock.findAllById(Set.of(blocking.getId()))).thenReturn(List.of(blocking));

		assertThat(selector.isBlocked(errandWearing(loaded(label(APPLICATION, null)), notLoaded(blocking.getId())))).isTrue();
		verify(metadataLabelRepositoryMock).findAllById(Set.of(blocking.getId()));
	}

	@Test
	@DisplayName("Verification that both answers are read out of labels looked up once, a block and a key alike")
	void bothAnswersAreReadOutOfOneLookup() {
		final var blocking = blocking("true");
		final var application = label(APPLICATION, "MANUAL");
		when(metadataLabelRepositoryMock.findAllById(Set.of(blocking.getId(), application.getId()))).thenReturn(List.of(blocking, application));

		final var labels = selector.read(errandWearing(notLoaded(blocking.getId()), notLoaded(application.getId())));

		assertThat(labels.blocked()).isTrue();
		assertThat(labels.selection()).isEqualTo(new ProcessKeySelection(APPLICATION, MANUAL, List.of(APPLICATION)));
		verify(metadataLabelRepositoryMock).findAllById(Set.of(blocking.getId(), application.getId()));
		verifyNoMoreInteractions(metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that labels blocking nothing are read as no block, beside what they select")
	void labelsBlockingNothingAreReadAsNoBlock() {
		final var labels = selector.read(errandWith(label(APPLICATION, null)));

		assertThat(labels.blocked()).isFalse();
		assertThat(labels.selection().processKey()).isEqualTo(APPLICATION);
		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a deprecated label with processBlocked=true still blocks, unlike a deprecated label naming a process")
	void aDeprecatedBlockingLabelStillBlocks() {
		assertThat(selector.isBlocked(errandWith(blocking("true").withDeprecated(true)))).isTrue();
	}

	@ParameterizedTest
	@MethodSource("valuesThatDoNotBlock")
	@DisplayName("Verification that processBlocked blocks only when its value is exactly true")
	void onlyExactlyTrueBlocks(final String value) {
		assertThat(selector.isBlocked(errandWith(blocking(value)))).isFalse();
	}

	private static Stream<Arguments> valuesThatDoNotBlock() {
		return Stream.of(
			arguments("false"),
			arguments("TRUE"),
			arguments(" true"),
			arguments("yes"));
	}

	@Test
	@DisplayName("Verification that an errand without labels, or with labels saying nothing about a block, is not blocked")
	void anErrandWithoutABlockingLabelIsNotBlocked() {
		assertThat(selector.isBlocked(ErrandEntity.create())).isFalse();
		assertThat(selector.isBlocked(errandWith(label(APPLICATION, null)))).isFalse();
		assertThat(selector.blockingLabelIdsOf(List.of())).isEmpty();
		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	private MetadataLabelEntity blocking(final String value) {
		return MetadataLabelEntity.create()
			.withId(randomUUID().toString())
			.withAttributes(List.of(attribute(PROCESS_BLOCKED_ATTRIBUTE, value)));
	}

	private ErrandEntity errandWith(final MetadataLabelEntity... labels) {
		return errandWearing(Stream.of(labels).map(this::loaded).toArray(ErrandLabelEmbeddable[]::new));
	}

	private ErrandEntity errandWearing(final ErrandLabelEmbeddable... labels) {
		return ErrandEntity.create().withLabels(new ArrayList<>(List.of(labels)));
	}

	/**
	 * A label as Hibernate hands it over when the errand is read from the database.
	 */
	private ErrandLabelEmbeddable loaded(final MetadataLabelEntity label) {
		final var embeddable = ErrandLabelEmbeddable.create().withMetadataLabelId(label.getId());
		ReflectionTestUtils.setField(embeddable, "metadataLabel", label);
		return embeddable;
	}

	/**
	 * A label as the mapper puts it together, before the errand has ever been loaded.
	 */
	private ErrandLabelEmbeddable notLoaded(final String metadataLabelId) {
		return ErrandLabelEmbeddable.create().withMetadataLabelId(metadataLabelId);
	}

	private MetadataLabelEntity label(final String processKey, final String startMode) {
		final var attributes = new ArrayList<LabelAttributeEmbeddable>();
		attributes.add(attribute(PROCESS_KEY_ATTRIBUTE, processKey));

		if (startMode != null) {
			attributes.add(attribute(PROCESS_START_MODE_ATTRIBUTE, startMode));
		}

		return MetadataLabelEntity.create()
			.withId(randomUUID().toString())
			.withAttributes(attributes);
	}

	private LabelAttributeEmbeddable attribute(final String key, final String value) {
		return LabelAttributeEmbeddable.create().withKey(key).withValue(value);
	}
}
