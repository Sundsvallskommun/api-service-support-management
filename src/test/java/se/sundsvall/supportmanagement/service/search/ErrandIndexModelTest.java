package se.sundsvall.supportmanagement.service.search;

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

	@Test
	void aNameTheIndexDoesNotHoldKeepsTheServiceFromStarting() {
		when(descriptorMock.field(anyString())).thenAnswer(invocation -> "title".equals(invocation.getArgument(0)) ? Optional.empty() : Optional.of(anyOther(invocation.getArgument(0))));

		assertThatThrownBy(() -> new ErrandIndexModel(descriptorMock))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("The errand index does not hold what is declared on it: 'title' declared on field TITLE is not a field of the index");
	}
}
