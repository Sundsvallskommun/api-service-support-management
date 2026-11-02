package se.sundsvall.supportmanagement.integration.db.search;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import se.sundsvall.supportmanagement.model.enums.ErrandField;
import se.sundsvall.supportmanagement.model.enums.ProtectedResource;

import static java.util.stream.Collectors.toMap;

/**
 * How the search index holds the fields of an errand and the resources belonging to it. What a role keeps from a user
 * of a field or a resource, it keeps from them in a search, by the index fields named here.
 */
public final class ErrandIndexBindings {

	private static final Map<ErrandField, IndexBinding> FIELDS = Stream.of(ErrandField.values())
		.collect(toMap(field -> field, ErrandIndexBindings::bindingOf, (first, _) -> first, () -> new EnumMap<>(ErrandField.class)));

	private static final Map<ProtectedResource, List<String>> RESOURCES = Stream.of(ProtectedResource.values())
		.collect(toMap(resource -> resource, ErrandIndexBindings::searchFieldsOf, (first, _) -> first, () -> new EnumMap<>(ProtectedResource.class)));

	private ErrandIndexBindings() {}

	/** How the search index holds sent in field. */
	public static IndexBinding of(final ErrandField field) {
		return FIELDS.get(field);
	}

	/** The index fields that hold sent in field, see {@link IndexBinding#fields()}. */
	public static List<String> searchFields(final ErrandField field) {
		return of(field).fields();
	}

	/**
	 * The index field an ordering by sent in property sorts on, empty when the property is not one of sent in field or
	 * cannot be sorted on.
	 */
	public static Optional<String> sortField(final ErrandField field, final String property) {
		return Optional.ofNullable(of(field).sorts().get(keyOf(field, property)));
	}

	/** The properties an ordering may name for sent in field. */
	public static List<String> sortableProperties(final ErrandField field) {
		return propertiesOf(field, of(field).sorts());
	}

	/**
	 * The index field a count grouping by sent in property groups by, empty when the property is not one of sent in field
	 * or cannot be grouped by.
	 */
	public static Optional<String> groupField(final ErrandField field, final String property) {
		return Optional.ofNullable(of(field).groups().get(keyOf(field, property)));
	}

	/** The properties a count may group by for sent in field. */
	public static List<String> groupableProperties(final ErrandField field) {
		return propertiesOf(field, of(field).groups());
	}

	/**
	 * The fields of the search index that hold sent in resource, by their names or, for a name ending in a dot, by the
	 * start of their names. Empty for a resource the index does not hold, which is every resource not belonging to an
	 * errand and the errand itself, whose own fields are bound by {@link #of(ErrandField)}. What is guarded on the
	 * resource is guarded on these in a search.
	 */
	public static List<String> searchFields(final ProtectedResource resource) {
		return RESOURCES.get(resource);
	}

	private static String keyOf(final ErrandField field, final String property) {
		return field.getPropertyName().equals(property) ? IndexBinding.OWN_PROPERTY : property;
	}

	private static List<String> propertiesOf(final ErrandField field, final Map<String, String> byProperty) {
		return byProperty.keySet().stream()
			.map(key -> IndexBinding.OWN_PROPERTY.equals(key) ? field.getPropertyName() : key)
			.toList();
	}

	private static IndexBinding bindingOf(final ErrandField field) {
		return switch (field) {
			case ID -> IndexBinding.none();
			case ERRAND_NUMBER -> IndexBinding.of(ErrandIndex.ERRAND_NUMBER).sortedBy(ErrandIndex.ERRAND_NUMBER);
			case TITLE -> IndexBinding.of(ErrandIndex.TITLE).sortedBy(ErrandIndex.TITLE_SORT);
			case STATUS -> IndexBinding.of(ErrandIndex.STATUS).sortedBy(ErrandIndex.STATUS).groupedBy(ErrandIndex.STATUS);
			case LIFECYCLE -> IndexBinding.of(ErrandIndex.LIFECYCLE);
			case RESOLUTION -> IndexBinding.of(ErrandIndex.RESOLUTION).sortedBy(ErrandIndex.RESOLUTION).groupedBy(ErrandIndex.RESOLUTION);
			case CHANNEL -> IndexBinding.of(ErrandIndex.CHANNEL).sortedBy(ErrandIndex.CHANNEL).groupedBy(ErrandIndex.CHANNEL);
			case CREATED -> IndexBinding.of(ErrandIndex.CREATED).sortedBy(ErrandIndex.CREATED);
			case MODIFIED -> IndexBinding.of(ErrandIndex.MODIFIED).sortedBy(ErrandIndex.MODIFIED);
			case TOUCHED -> IndexBinding.of(ErrandIndex.TOUCHED).sortedBy(ErrandIndex.TOUCHED);
			case DELETED -> IndexBinding.of(ErrandIndex.DELETED).sortedBy(ErrandIndex.DELETED);
			case DELETED_BY -> IndexBinding.of(ErrandIndex.DELETED_BY);
			case PRIORITY -> IndexBinding.of(ErrandIndex.PRIORITY).sortedBy(ErrandIndex.PRIORITY).groupedBy(ErrandIndex.PRIORITY);
			case DESCRIPTION -> IndexBinding.of(ErrandIndex.DESCRIPTION);
			case CLASSIFICATION -> IndexBinding.of(ErrandIndex.CATEGORY, ErrandIndex.TYPE)
				.sortedBy("category", ErrandIndex.CATEGORY)
				.sortedBy("type", ErrandIndex.TYPE)
				.groupedBy("category", ErrandIndex.CATEGORY)
				.groupedBy("type", ErrandIndex.TYPE);
			case REPORTER_USER_ID -> IndexBinding.of(ErrandIndex.REPORTER_USER_ID).sortedBy(ErrandIndex.REPORTER_USER_ID).groupedBy(ErrandIndex.REPORTER_USER_ID);
			case ASSIGNED_USER_ID -> IndexBinding.of(ErrandIndex.ASSIGNED_USER_ID).sortedBy(ErrandIndex.ASSIGNED_USER_ID).groupedBy(ErrandIndex.ASSIGNED_USER_ID);
			case ASSIGNED_GROUP_ID -> IndexBinding.of(ErrandIndex.ASSIGNED_GROUP_ID).sortedBy(ErrandIndex.ASSIGNED_GROUP_ID).groupedBy(ErrandIndex.ASSIGNED_GROUP_ID);
			case BUSINESS_RELATED -> IndexBinding.of(ErrandIndex.BUSINESS_RELATED);
			case SUSPENSION -> IndexBinding.of(ErrandIndex.SUSPENDED_FROM, ErrandIndex.SUSPENDED_TO)
				.sortedBy("suspendedFrom", ErrandIndex.SUSPENDED_FROM)
				.sortedBy("suspendedTo", ErrandIndex.SUSPENDED_TO);
			case CONTACT_REASON -> IndexBinding.of(ErrandIndex.under(ErrandIndex.CONTACT_REASON));
			case CONTACT_REASON_DESCRIPTION -> IndexBinding.of(ErrandIndex.CONTACT_REASON_DESCRIPTION);
			case ESCALATION_EMAIL -> IndexBinding.of(ErrandIndex.ESCALATION_EMAIL);
			case LABELS -> IndexBinding.of(ErrandIndex.under(ErrandIndex.LABELS));
			case STAKEHOLDERS -> IndexBinding.of(ErrandIndex.under(ErrandIndex.STAKEHOLDERS));
			case MEASURES -> IndexBinding.of(ErrandIndex.under(ErrandIndex.MEASURES));
			case ACTIVE_NOTIFICATIONS -> IndexBinding.none();
			case PHASES -> IndexBinding.of(ErrandIndex.under(ErrandIndex.PHASES));
			case ACTIONS -> IndexBinding.none();
			case PROCESS -> IndexBinding.none();
			case VERSION -> IndexBinding.none();
			case PARAMETERS -> IndexBinding.of(ErrandIndex.under(ErrandIndex.PARAMETERS));
			case JSON_PARAMETERS -> IndexBinding.of(ErrandIndex.under(ErrandIndex.JSON_PARAMETERS), ErrandIndex.JSON_PARAMETERS_TEXT).withKeysAsPaths();
			case EXTERNAL_TAGS -> IndexBinding.of(ErrandIndex.under(ErrandIndex.EXTERNAL_TAGS));
		};
	}

	private static List<String> searchFieldsOf(final ProtectedResource resource) {
		return switch (resource) {
			case ATTACHMENT -> List.of(ErrandIndex.under(ErrandIndex.ATTACHMENTS));
			case COMMUNICATION -> List.of(ErrandIndex.under(ErrandIndex.COMMUNICATIONS));
			case PARAMETER -> List.of(ErrandIndex.under(ErrandIndex.PARAMETERS));
			case JSON_PARAMETER -> List.of(ErrandIndex.under(ErrandIndex.JSON_PARAMETERS), ErrandIndex.JSON_PARAMETERS_TEXT);
			case MEASURE -> List.of(ErrandIndex.under(ErrandIndex.MEASURES));
			case STATEMENT -> List.of(ErrandIndex.under(ErrandIndex.STATEMENTS));
			case INVESTIGATION -> List.of(ErrandIndex.under(ErrandIndex.INVESTIGATIONS));
			case DECISION -> List.of(ErrandIndex.under(ErrandIndex.DECISIONS));
			default -> List.of();
		};
	}
}
