package se.sundsvall.supportmanagement.service.util;

import com.turkraft.springfilter.parser.node.FieldNode;
import com.turkraft.springfilter.parser.node.FilterNode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.Pageable;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Guards the filter and the ordering of the errand listing against the associations the errand holds for the search
 * index alone, see {@link ErrandEntity#INDEX_ONLY_ASSOCIATIONS}: spring-filter would let a filter walk them just as
 * readily as the stakeholders, and they are guarded on resources of their own.
 * <p>
 * The filter is looked into as parsed, which holds every filter parameter of a request, since spring-filter joins
 * them into one tree. The ordering reaches the associations by another door. They are properties of the entity now, so
 * Spring Data resolves a sort on one of them into a join of the collection, which both multiplies the errands of a page
 * against the count query beside it and orders the listing by data belonging to a resource of its own.
 */
public final class ErrandFilters {

	static final String NOT_FILTERABLE = "Filtering on '%s' is not supported";
	static final String NOT_SORTABLE = "Sorting on '%s' is not supported";

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
			indexOnlyAssociationIn(order.getProperty()).ifPresent(association -> {
				throw Problem.valueOf(BAD_REQUEST, NOT_SORTABLE.formatted(association));
			});
		}
	}

	// A field anywhere in the tree, values being nodes of their own. spring-filter asks whether a collection is empty and
	// how large it is with a field naming the association alone, so a field of it need not follow
	private static Optional<String> indexOnlyAssociationIn(final FilterNode node) {
		if (isNull(node)) {
			return Optional.empty();
		}
		if (node instanceof final FieldNode fieldNode) {
			final var association = indexOnlyAssociationIn(fieldNode.getName());
			if (association.isPresent()) {
				return association;
			}
		}
		return ofNullable(node.getChildren()).orElse(List.of()).stream()
			.map(ErrandFilters::indexOnlyAssociationIn)
			.flatMap(Optional::stream)
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
