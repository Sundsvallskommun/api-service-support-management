package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.LabelAttributeEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_KEY_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_START_MODE_ATTRIBUTE;

class ProcessKeySelectorTest {

	private static final String APPLICATION = "alkt-ansokan";
	private static final String SUPERVISION = "alkt-tillsyn";

	private final ProcessKeySelector selector = new ProcessKeySelector();

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
	@DisplayName("Verification that a label pointing at nothing, which a partially loaded errand can carry, is passed over rather than thrown on")
	void anEmbeddableWithoutALabelIsPassedOver() {
		final var errand = ErrandEntity.create().withLabels(new ArrayList<>(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(randomUUID().toString()))));

		assertThat(selector.select(errand).keys()).isEmpty();
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
	@DisplayName("Verification that a MANUAL on one of two labels naming the same process wins over the other label's silence")
	void aManualAmongLabelsSharingAKeyWins() {
		final var selection = selector.select(errandWith(label(APPLICATION, null), label(APPLICATION, "MANUAL")));

		assertThat(selection.processKey()).isEqualTo(APPLICATION);
		assertThat(selection.startMode()).isEqualTo(MANUAL);
	}

	private ErrandEntity errandWith(final MetadataLabelEntity... labels) {
		return ErrandEntity.create().withLabels(Stream.of(labels)
			.map(label -> {
				final var embeddable = ErrandLabelEmbeddable.create().withMetadataLabelId(label.getId());
				ReflectionTestUtils.setField(embeddable, "metadataLabel", label);
				return embeddable;
			})
			.toList());
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
