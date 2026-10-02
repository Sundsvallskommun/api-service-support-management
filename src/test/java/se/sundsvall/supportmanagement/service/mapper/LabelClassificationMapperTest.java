package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.LabelClassification;
import se.sundsvall.supportmanagement.integration.db.model.LabelClassificationEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.applyClassificationDisplayNames;
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.toLabelClassification;
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.toLabelClassificationEntity;
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.updateLabelClassificationEntity;

class LabelClassificationMapperTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Test
	void toLabelClassificationFromEntity() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var entity = LabelClassificationEntity.create()
			.withId("id")
			.withClassification("subtype")
			.withDisplayName("Undertyp")
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withCreated(created)
			.withModified(modified);

		final var result = toLabelClassification(entity);

		assertThat(result).isEqualTo(LabelClassification.create()
			.withId("id")
			.withClassification("subtype")
			.withDisplayName("Undertyp")
			.withCreated(created)
			.withModified(modified));
	}

	@Test
	void toLabelClassificationFromNull() {
		assertThat(toLabelClassification(null)).isNull();
	}

	@Test
	void toLabelClassificationEntityFromModel() {
		final var result = toLabelClassificationEntity(NAMESPACE, MUNICIPALITY_ID, LabelClassification.create().withClassification("subtype").withDisplayName("Undertyp"));

		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getClassification()).isEqualTo("subtype");
		assertThat(result.getDisplayName()).isEqualTo("Undertyp");
		assertThat(result.getId()).isNull();
	}

	@Test
	void toLabelClassificationEntityFromNull() {
		assertThat(toLabelClassificationEntity(NAMESPACE, MUNICIPALITY_ID, null)).isNull();
		assertThat(toLabelClassificationEntity(null, MUNICIPALITY_ID, LabelClassification.create())).isNull();
		assertThat(toLabelClassificationEntity(NAMESPACE, null, LabelClassification.create())).isNull();
	}

	@Test
	void updateLabelClassificationEntityChangesDisplayNameOnly() {
		final var entity = LabelClassificationEntity.create().withClassification("subtype").withDisplayName("Old");

		final var result = updateLabelClassificationEntity(entity, LabelClassification.create().withClassification("other").withDisplayName("New"));

		assertThat(result).isSameAs(entity);
		assertThat(result.getClassification()).isEqualTo("subtype");
		assertThat(result.getDisplayName()).isEqualTo("New");
	}

	@Test
	void updateLabelClassificationEntityLeavesDisplayNameWhenNotInPatch() {
		final var entity = LabelClassificationEntity.create().withClassification("subtype").withDisplayName("Old");

		assertThat(updateLabelClassificationEntity(entity, LabelClassification.create()).getDisplayName()).isEqualTo("Old");
		assertThat(updateLabelClassificationEntity(entity, null).getDisplayName()).isEqualTo("Old");
	}

	@Test
	void applyClassificationDisplayNamesToLabelTree() {
		final var leaf = Label.create().withClassification("subtype");
		final var unmapped = Label.create().withClassification("unmapped");
		final var noClassification = Label.create();
		final var root = Label.create().withClassification("category").withLabels(List.of(leaf, unmapped, noClassification));

		applyClassificationDisplayNames(List.of(root), Map.of("category", "Kategori", "subtype", "Undertyp"));

		assertThat(root.getClassificationDisplayName()).isEqualTo("Kategori");
		assertThat(leaf.getClassificationDisplayName()).isEqualTo("Undertyp");
		assertThat(unmapped.getClassificationDisplayName()).isNull();
		assertThat(noClassification.getClassificationDisplayName()).isNull();
	}

	@Test
	void applyClassificationDisplayNamesToLabelTreeHandlesNull() {
		final var label = Label.create().withClassification("subtype");

		applyClassificationDisplayNames((List<Label>) null, Map.of());
		applyClassificationDisplayNames(List.of(label), null);

		assertThat(label.getClassificationDisplayName()).isNull();
	}

	@Test
	void applyClassificationDisplayNamesToErrand() {
		final var errand = Errand.create().withLabels(List.of(
			ErrandLabel.create().withId("1").withClassification("subtype"),
			ErrandLabel.create().withId("2").withClassification("unmapped")));

		final var result = applyClassificationDisplayNames(errand, Map.of("subtype", "Undertyp"));

		assertThat(result).isSameAs(errand);
		assertThat(result.getLabels())
			.extracting(ErrandLabel::getId, ErrandLabel::getClassificationDisplayName)
			.containsExactly(tuple("1", "Undertyp"), tuple("2", null));
	}

	@Test
	void applyClassificationDisplayNamesToErrandWithoutLabels() {
		final var errand = Errand.create();

		assertThat(applyClassificationDisplayNames(errand, Map.of("subtype", "Undertyp"))).isSameAs(errand);
		assertThat(errand.getLabels()).isNull();
		assertThat(applyClassificationDisplayNames((Errand) null, Map.of())).isNull();
	}
}
