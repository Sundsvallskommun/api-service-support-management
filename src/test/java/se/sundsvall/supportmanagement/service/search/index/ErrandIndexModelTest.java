package se.sundsvall.supportmanagement.service.search.index;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.search.SearchAnalysisConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The real index is checked by the integration tests, which start with search enabled; this covers what the model does
 * with a descriptor that does or does not hold what is declared.
 */
@ExtendWith(MockitoExtension.class)
class ErrandIndexModelTest {

	@Mock
	private IndexDescriptor descriptorMock;

	private static IndexFieldDescriptor valueField(final String path, final String analyzer, final boolean sortable, final Class<?> dslClass) {
		final var type = mock(IndexValueFieldTypeDescriptor.class);
		lenient().when(type.analyzerName()).thenReturn(Optional.ofNullable(analyzer));
		lenient().when(type.sortable()).thenReturn(sortable);
		lenient().doReturn(dslClass).when(type).dslArgumentClass();
		final var field = mock(IndexValueFieldDescriptor.class);
		lenient().when(field.isValueField()).thenReturn(true);
		lenient().when(field.isObjectField()).thenReturn(false);
		lenient().when(field.absolutePath()).thenReturn(path);
		lenient().when(field.toValueField()).thenReturn(field);
		lenient().when(field.type()).thenReturn(type);
		return field;
	}

	private static IndexFieldDescriptor objectField(final String path) {
		final var field = mock(IndexFieldDescriptor.class);
		lenient().when(field.isValueField()).thenReturn(false);
		lenient().when(field.isObjectField()).thenReturn(true);
		lenient().when(field.absolutePath()).thenReturn(path);
		return field;
	}

	/** Any other name the bindings declare: the objects as objects, the rest as sortable keyword fields. */
	private static IndexFieldDescriptor anyOther(final String path) {
		final var objects = Set.of("contactReason", "labels", "stakeholders", "measures", "phases", "parameters", "jsonParameters", "externalTags", "attachments", "communications", "decisions", "statements", "investigations");
		return objects.contains(path) ? objectField(path) : valueField(path, null, true, String.class);
	}

	@Test
	void withoutAnIndexNothingIsSearched() {
		assertThat(new ErrandIndexModel(null).textFields()).isEmpty();
	}

	@Test
	void textFieldsAreTheAnalyzedOnesAndTheIdentifiers() {
		final var title = valueField("title", SearchAnalysisConfigurer.TEXT, false, String.class);
		final var deep = valueField("measures.description", SearchAnalysisConfigurer.TEXT, false, String.class);
		final var keyword = valueField("status", null, true, String.class);
		final var sortable = valueField("title_sort", null, true, String.class);
		final var date = valueField("created", null, false, JsonElement.class);
		final var stakeholders = objectField("stakeholders");
		when(descriptorMock.staticFields()).thenReturn(List.of(title, deep, keyword, sortable, date, stakeholders));
		// Every name declared exists, and what is declared sortable is
		when(descriptorMock.field(anyString())).thenAnswer(invocation -> Optional.of(switch (invocation.<String>getArgument(0)) {
			case "title" -> title;
			case "title_sort" -> sortable;
			case "created" -> date;
			case "status" -> keyword;
			default -> anyOther(invocation.getArgument(0));
		}));

		final var model = new ErrandIndexModel(descriptorMock);

		assertThat(model.textFields()).containsExactly("errandNumber", "externalTags.value", "measures.description", "stakeholders.externalId", "title");
	}

	/**
	 * A field the index holds that no field of an errand and no resource names is one nothing can grant, so nothing can
	 * search it. Safe, and quietly wrong where the field was meant to be searchable, which is why it is said at startup.
	 */
	@Test
	void aFieldTheIndexHoldsAndNothingBindsKeepsTheServiceFromStarting() {
		final var unbound = valueField("previousStatus", null, true, String.class);
		when(descriptorMock.staticFields()).thenReturn(List.of(unbound));
		when(descriptorMock.field(anyString())).thenAnswer(invocation -> Optional.of(anyOther(invocation.getArgument(0))));

		assertThatThrownBy(() -> new ErrandIndexModel(descriptorMock))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("'previousStatus' is held by the index and bound to no field or resource, so nothing can grant it");
	}

	/**
	 * What the index keeps for itself, the two fields the search is filtered by, and what access control counts its labels
	 * with: fields of the index that belong to no field of an errand, and are named as such rather than bound.
	 */
	@Test
	void theIndexOwnFieldsAndTheBookkeepingOfAccessControlAreAccepted() {
		final var id = valueField("_id", null, true, String.class);
		final var entityType = valueField("_entity_type", null, true, String.class);
		final var municipality = valueField("municipalityId", null, true, String.class);
		final var namespace = valueField("namespace", null, true, String.class);
		final var labelId = valueField("accessLabels.metadataLabelId", null, true, String.class);
		final var count = valueField("accessLabelCount", null, true, Integer.class);
		when(descriptorMock.staticFields()).thenReturn(List.of(id, entityType, municipality, namespace, labelId, count));
		when(descriptorMock.field(anyString())).thenAnswer(invocation -> Optional.of(anyOther(invocation.getArgument(0))));

		assertThatCode(() -> new ErrandIndexModel(descriptorMock)).doesNotThrowAnyException();
	}

	@Test
	void aNameTheIndexDoesNotHoldKeepsTheServiceFromStarting() {
		when(descriptorMock.field(anyString())).thenAnswer(invocation -> "title".equals(invocation.getArgument(0)) ? Optional.empty() : Optional.of(anyOther(invocation.getArgument(0))));

		assertThatThrownBy(() -> new ErrandIndexModel(descriptorMock))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("The errand index does not hold what is declared on it: 'title' declared on field TITLE is not a field of the index");
	}
}
