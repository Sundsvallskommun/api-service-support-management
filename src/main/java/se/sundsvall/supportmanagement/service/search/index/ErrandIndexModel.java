package se.sundsvall.supportmanagement.service.search.index;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
 * are checked against the index here, once, when the service starts: a name the index does not know, a sort on a field
 * that cannot be sorted on, or a field of the index nothing binds switches search off on this instance rather than
 * turning up as an empty search. The service starts either way - nothing else it does depends on the index - and the
 * endpoints answer 503 while the health reports what was found.
 */
@Component
public class ErrandIndexModel {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandIndexModel.class);

	/**
	 * Fields the index holds that belong to no field of an errand: the two the search is filtered by, what access
	 * control counts its own labels with, and the field a word looks in when a route opens none.
	 */
	private static final Set<String> NOT_OF_AN_ERRAND = Set.of(ErrandIndex.MUNICIPALITY_ID, ErrandIndex.NAMESPACE, ErrandIndex.ACCESS_LABEL_ID, ErrandIndex.ACCESS_LABEL_COUNT,
		ErrandIndex.NO_OPEN_FIELD);

	private final List<String> textFields;

	@Autowired
	public ErrandIndexModel(final OpenSearchClient openSearch, final SearchAvailability availability) {
		this(availability.isEnabled() ? openSearch.errandIndex() : null, availability);
	}

	/**
	 * An index that does not hold what the bindings name is a defect of this service, not of the cluster, and the
	 * bindings are what access control is rendered from - so search is given up on rather than run against an index it
	 * misreads. Said as loudly as a log allows and answered with 503, rather than by keeping the service from starting:
	 * everything else it does is beside the point of the search index, and none of it should wait for this to be put
	 * right.
	 *
	 * @param descriptor   the index, null where the environment has none
	 * @param availability what search is given up on through, null where nothing is to be told
	 */
	ErrandIndexModel(final IndexDescriptor descriptor, final SearchAvailability availability) {
		if (descriptor == null) {
			textFields = List.of();
			return;
		}

		try {
			validate(descriptor);
		} catch (final IllegalStateException e) {
			LOG.error("Search is switched off on this instance: {}", e.getMessage(), e);
			if (availability != null) {
				availability.giveUp(e.getMessage());
			}
			textFields = List.of();
			return;
		}

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
	 * The index field a count grouping by sent in property groups by, empty when no field of the errand offers the
	 * property for grouping.
	 */
	public static Optional<String> groupField(final String property) {
		return Stream.of(ErrandField.values())
			.map(field -> field.getGroupField(property))
			.flatMap(Optional::stream)
			.findFirst();
	}

	/** The properties a count may group by, sorted. */
	public static List<String> groupableProperties() {
		return Stream.of(ErrandField.values())
			.flatMap(field -> field.getGroupableProperties().stream())
			.sorted()
			.toList();
	}

	/** The field of the errand sent in groupable property belongs to, which is what a route's grant is asked about. */
	public static Optional<ErrandField> groupedField(final String property) {
		return Stream.of(ErrandField.values())
			.filter(field -> field.getGroupField(property).isPresent())
			.findFirst();
	}

	/**
	 * Holds every name the bindings and the search refer to against the index.
	 */
	private static void validate(final IndexDescriptor descriptor) {
		final var problems = new ArrayList<String>();

		for (final var field : ErrandField.values()) {
			field.getSearchFields().forEach(name -> verifyExists(descriptor, name, "field " + field, problems));
			field.getIndex().sorts().values().forEach(name -> verifySortable(descriptor, name, "field " + field, problems));
			field.getIndex().groups().values().forEach(name -> verifyAggregatable(descriptor, name, "field " + field, problems));
		}
		for (final var resource : ProtectedResource.values()) {
			resource.getSearchFields().forEach(name -> verifyExists(descriptor, name, "resource " + resource, problems));
		}
		ErrandIndex.IDENTIFIER_FIELDS.forEach(name -> verifyExists(descriptor, name, "identifier fields", problems));
		verifyEverythingIsBound(descriptor, problems);

		if (!problems.isEmpty()) {
			throw new IllegalStateException("The errand index does not hold what is declared on it: " + problems.stream().collect(joining("; ")));
		}
	}

	/**
	 * That every field of the index belongs to a field of the errand or to a resource, and can therefore be granted.
	 * <p>
	 * A search may name only what a route grants, so a field bound to nothing is a field nobody can search - which is
	 * safe, and quietly wrong if the field was meant to be searchable. Said at startup rather than found later: the one
	 * thing neither the compiler nor the access rules can notice is a field that was indexed and then left out of both.
	 * The tenancy of the index and the bookkeeping of access control are named here as what they are, fields of the index
	 * that belong to no field of an errand.
	 */
	private static void verifyEverythingIsBound(final IndexDescriptor descriptor, final List<String> problems) {
		final var bound = new HashSet<String>();
		for (final var field : ErrandField.values()) {
			bound.addAll(field.getSearchFields());
			bound.addAll(field.getIndex().sorts().values());
			bound.addAll(field.getIndex().groups().values());
		}
		for (final var resource : ProtectedResource.values()) {
			bound.addAll(resource.getSearchFields());
		}
		bound.addAll(ErrandIndex.IDENTIFIER_FIELDS);
		bound.addAll(NOT_OF_AN_ERRAND);

		descriptor.staticFields().stream()
			.filter(IndexFieldDescriptor::isValueField)
			.map(IndexFieldDescriptor::absolutePath)
			// What the index keeps for itself, named as the index names such things
			.filter(name -> !name.startsWith("_"))
			.filter(name -> bound.stream().noneMatch(binding -> covers(binding, name)))
			.forEach(name -> problems.add("'%s' is held by the index and bound to no field or resource, so nothing can grant it".formatted(name)));
	}

	/**
	 * Whether a binding covers sent in name: a name ending in a dot stands for the object and everything under it, which
	 * is how {@code SearchableFields} reads a binding when it decides what may be searched.
	 */
	private static boolean covers(final String binding, final String name) {
		if (!binding.endsWith(".")) {
			return name.equals(binding);
		}
		return name.equals(binding.substring(0, binding.length() - 1)) || name.startsWith(binding);
	}

	private static void verifyExists(final IndexDescriptor descriptor, final String name, final String declaredOn, final List<String> problems) {
		final var object = name.endsWith(".");
		final var field = descriptor.field(object ? name.substring(0, name.length() - 1) : name);
		// A start of names is an object of the index, or a native field mapped as one, which is how the JSON parameters are
		// held; a name is a field of a value
		final boolean holds = field.map(found -> object ? found.isObjectField() || isNative(found) : found.isValueField()).orElse(false);
		if (!holds) {
			problems.add("'%s' declared on %s is not %s of the index".formatted(name, declaredOn, object ? "an object" : "a field"));
		}
	}

	private static boolean isNative(final IndexFieldDescriptor field) {
		return field.isValueField() && JsonElement.class.equals(field.toValueField().type().dslArgumentClass());
	}

	/**
	 * That a field a count groups by can be aggregated, which is what a terms aggregation over it needs. Said at startup
	 * rather than as a 500 on the first grouped count, since the annotation granting it sits a long way from the binding
	 * naming it.
	 */
	private static void verifyAggregatable(final IndexDescriptor descriptor, final String name, final String declaredOn, final List<String> problems) {
		final var field = descriptor.field(name).filter(IndexFieldDescriptor::isValueField);
		if (field.isEmpty()) {
			problems.add("'%s' declared as a group on %s is not a field of the index".formatted(name, declaredOn));
			return;
		}
		if (!field.get().toValueField().type().aggregable() && !isNative(field.get())) {
			problems.add("'%s' declared as a group on %s cannot be aggregated".formatted(name, declaredOn));
		}
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
