package se.sundsvall.supportmanagement.service.util;

import com.turkraft.springfilter.parser.node.FilterNode;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.domain.Pageable;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Guards the filter and the ordering of the errand listing against the associations the errand holds for the search
 * index alone, see {@link ErrandEntity#INDEX_ONLY_ASSOCIATIONS}: spring-filter would let a filter walk them just as
 * readily as the stakeholders, and they are guarded on resources of their own.
 * <p>
 * The filter is looked into as parsed, which holds every filter parameter of a request, since spring-filter joins
 * them into one tree. The ordering reaches the associations by another door. They are properties of the entity, so
 * Spring
 * Data resolves a sort on one of them into a join of the collection, which both multiplies the errands of a page
 * against the count query beside it and orders the listing by data belonging to a resource of its own.
 */
public final class ErrandFilters {

	static final String NOT_FILTERABLE = "Filtering on '%s' is not supported";
	static final String NOT_SORTABLE = "Sorting on '%s' is not supported";

	// The entities behind the index-only associations, guarded on resources of their own
	private static final Set<Class<?>> GUARDED_ENTITIES = Set.of(DecisionEntity.class, StatementEntity.class, InvestigationEntity.class, CommunicationEntity.class);

	private ErrandFilters() {}

	/**
	 * @param  filter                                         the parsed filter of the request, or null
	 * @throws org.springframework.web.ErrorResponseException 400 when the filter of the request reaches an association
	 *                                                        that is not filterable
	 */
	public static void verifyFilterable(final FilterNode filter) {
		indexOnlyAssociationIn(filter).ifPresent(association -> {
			throw Problem.valueOf(BAD_REQUEST, NOT_FILTERABLE.formatted(association));
		});
	}

	/**
	 * @throws org.springframework.web.ErrorResponseException 400 when the ordering of the request names an association
	 *                                                        that is not sortable
	 */
	public static void verifySortable(final Pageable pageable) {
		if (isNull(pageable)) {
			return;
		}
		for (final var order : pageable.getSort()) {
			guardedAssociationIn(order.getProperty()).ifPresent(association -> {
				throw Problem.valueOf(BAD_REQUEST, NOT_SORTABLE.formatted(association));
			});
		}
	}

	// A field anywhere in the tree. spring-filter asks whether a collection is empty and how large it is with a field
	// naming the association alone, so a field of it need not follow
	private static Optional<String> indexOnlyAssociationIn(final FilterNode node) {
		return FilterFields.fieldsOf(node)
			.map(ErrandFilters::indexOnlyAssociationIn)
			.flatMap(Optional::stream)
			.findFirst();
	}

	// The ordering resolved as the repository resolves it, which lower-cases the first letter, takes an underscore for a
	// dot and splits camel case, and asked for the entities it reaches rather than for the names it spells. That holds
	// whichever way the association is reached, measures.decisionEntity as much as decisions. A property the errand does
	// not have is refused by PropertyPath just as the repository would refuse it
	private static Optional<String> guardedAssociationIn(final String property) {
		return PropertyPath.from(property, ErrandEntity.class).stream()
			.filter(segment -> GUARDED_ENTITIES.contains(segment.getType()))
			.map(PropertyPath::getSegment)
			.findFirst();
	}

	// Any segment of the path, not only the first: every child of an errand carries a reference back to it, so
	// stakeholders.errandEntity.communications reaches by a detour what communications names outright. A whole segment
	// only, which is what keeps a property merely starting with the same word, such as decisionsCount, usable
	private static Optional<String> indexOnlyAssociationIn(final String path) {
		return Stream.of(path.split("\\."))
			.filter(ErrandEntity.INDEX_ONLY_ASSOCIATIONS::contains)
			.findFirst();
	}
}
