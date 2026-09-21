package se.sundsvall.supportmanagement.service.search.index;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.search.ErrandIndex;
import se.sundsvall.supportmanagement.integration.db.search.SearchAnalysisConfigurer;

import static java.util.stream.Collectors.joining;

/**
 * What the errand index looks like, read from Hibernate Search rather than written down a second time.
 * <p>
 * The fields a word without a field is looked for in are every field analyzed as text, plus the identifiers of
 * {@link ErrandIndex#IDENTIFIER_FIELDS}; the field an ordering sorts on comes from the binding of the
 * {@link ErrandField} the ordering names. And since the bindings of the fields and resources name index fields, they
 * are checked against the index here, once, when the service starts: a name the index does not know, or a sort on a
 * field that cannot be sorted on, keeps the service from starting rather than turning up as an empty search.
 */
@Component
public class ErrandIndexModel {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandIndexModel.class);

	private final List<String> textFields;

	@Autowired
	public ErrandIndexModel(final OpenSearchClient openSearch, final SearchAvailability availability) {
		this(availability.isEnabled() ? openSearch.errandIndex() : null);
	}

	/**
	 * @param descriptor the index, null where the environment has none
	 */
	ErrandIndexModel(final IndexDescriptor descriptor) {
		if (descriptor == null) {
			textFields = List.of();
			return;
		}

		validate(descriptor);
		textFields = Stream.concat(
			descriptor.staticFields().stream()
				.filter(IndexFieldDescriptor::isValueField)
				.filter(field -> field.toValueField().type().analyzerName().filter(SearchAnalysisConfigurer.TEXT::equals).isPresent())
				.map(IndexFieldDescriptor::absolutePath),
			ErrandIndex.IDENTIFIER_FIELDS.stream())
			.sorted()
			.toList();
		LOG.info("Errand index holds {} fields, of which {} are searched for a word without a field", descriptor.staticFields().size(), textFields.size());
	}

	/**
	 * The fields a word without a field is looked for in. Empty where the environment has no search index.
	 */
	public List<String> textFields() {
		return textFields;
	}

	/**
	 * The index field an ordering by sent in property sorts on, empty when no field of the errand offers the property
	 * for sorting.
	 */
	public static Optional<String> sortField(final String property) {
		return Stream.of(ErrandField.values())
			.map(field -> field.getSortField(property))
			.flatMap(Optional::stream)
			.findFirst();
	}

	/** The properties an ordering may name, sorted. */
	public static List<String> sortableProperties() {
		return Stream.of(ErrandField.values())
			.flatMap(field -> field.getSortableProperties().stream())
			.sorted()
			.toList();
	}

	/**
	 * Holds every name the bindings and the search refer to against the index.
	 */
	private static void validate(final IndexDescriptor descriptor) {
		final var problems = new ArrayList<String>();

		for (final var field : ErrandField.values()) {
			field.getSearchFields().forEach(name -> verifyExists(descriptor, name, "field " + field, problems));
			field.getIndex().sorts().values().forEach(name -> verifySortable(descriptor, name, "field " + field, problems));
		}
		for (final var resource : ProtectedResource.values()) {
			resource.getSearchFields().forEach(name -> verifyExists(descriptor, name, "resource " + resource, problems));
		}
		ErrandIndex.IDENTIFIER_FIELDS.forEach(name -> verifyExists(descriptor, name, "identifier fields", problems));

		if (!problems.isEmpty()) {
			throw new IllegalStateException("The errand index does not hold what is declared on it: " + problems.stream().collect(joining("; ")));
		}
	}

	private static void verifyExists(final IndexDescriptor descriptor, final String name, final String declaredOn, final List<String> problems) {
		final var object = name.endsWith(".");
		final var field = descriptor.field(object ? name.substring(0, name.length() - 1) : name);
		// A start of names is an object of the index, or a native field mapped as one, which is how the JSON parameters are
		// held; a name is a field of a value
		final var holds = field.map(found -> object ? found.isObjectField() || isNative(found) : found.isValueField()).orElse(false);
		if (!holds) {
			problems.add("'%s' declared on %s is not %s of the index".formatted(name, declaredOn, object ? "an object" : "a field"));
		}
	}

	private static boolean isNative(final IndexFieldDescriptor field) {
		return field.isValueField() && JsonElement.class.equals(field.toValueField().type().dslArgumentClass());
	}

	private static void verifySortable(final IndexDescriptor descriptor, final String name, final String declaredOn, final List<String> problems) {
		final var field = descriptor.field(name).filter(IndexFieldDescriptor::isValueField);
		if (field.isEmpty()) {
			problems.add("'%s' declared as a sort on %s is not a field of the index".formatted(name, declaredOn));
			return;
		}
		// A native field is sorted on as JSON and Hibernate Search cannot tell whether it can be, so it is taken at its word
		if (!field.get().toValueField().type().sortable() && !isNative(field.get())) {
			problems.add("'%s' declared as a sort on %s cannot be sorted on".formatted(name, declaredOn));
		}
	}
}
