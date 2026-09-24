package se.sundsvall.supportmanagement.service.util;

import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.parser.node.FieldNode;
import com.turkraft.springfilter.parser.node.FilterNode;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Strings;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandLifecycle;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class SpecificationBuilder<T> {

	private static final SpecificationBuilder<ErrandEntity> ERRAND_ENTITY_BUILDER = new SpecificationBuilder<>();
	private static final String ACCESS_LABELS_ATTRIBUTE = "accessLabels";
	private static final String ID_ATTRIBUTE = "id";
	private static final String LIFECYCLE_ATTRIBUTE = "lifecycle";
	private static final String METADATA_LABEL_ID_ATTRIBUTE = "metadataLabelId";
	private static final String REPORTER_USER_ID_ATTRIBUTE = "reporterUserId";
	private static final String TOUCHED_ATTRIBUTE = "touched";
	private static final String MODIFIED_ATTRIBUTE = "modified";
	private static final String CREATED_ATTRIBUTE = "created";

	public static Specification<ErrandEntity> withNamespace(String namespace) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("namespace", namespace);
	}

	public static Specification<ErrandEntity> withMunicipalityId(String municipalityId) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("municipalityId", municipalityId);
	}

	public static Specification<ErrandEntity> withId(String id) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("id", id);
	}

	public static Specification<ErrandEntity> withLifecycle(ErrandLifecycle lifecycle) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter(LIFECYCLE_ATTRIBUTE, lifecycle);
	}

	/**
	 * Narrows a search to the active errands, unless the filter of the search says something about the life cycle itself.
	 * Drafts are thereby left out of a search that does not ask for them.
	 * <p>
	 * Only a filter parsed from the filter parameter of a request is looked into. Any other specification is taken to say
	 * nothing about the life cycle.
	 *
	 * @param  filter the filter of the search, or null
	 * @return        specification matching the active errands, or every errand when the filter names the life cycle
	 */
	public static Specification<ErrandEntity> withDefaultLifecycle(Specification<ErrandEntity> filter) {
		return filter instanceof final FilterSpecification<ErrandEntity> filterSpecification && namesField(filterSpecification.getFilter(), LIFECYCLE_ATTRIBUTE)
			? (_, _, criteriaBuilder) -> criteriaBuilder.and()
			: withLifecycle(ErrandLifecycle.ACTIVE);
	}

	/**
	 * Matches errands that have not been touched since the sent in point in time.
	 * <p>
	 * The first of touched, modified and created that is set says when an errand was last touched. An errand carrying
	 * none of them is left out.
	 *
	 * @param  cutoff the point in time an errand must have been untouched since
	 * @return        specification matching errands last touched before the sent in point in time
	 */
	public static Specification<ErrandEntity> withLastTouchedBefore(OffsetDateTime cutoff) {
		return (root, _, criteriaBuilder) -> criteriaBuilder.lessThan(criteriaBuilder.<OffsetDateTime>coalesce()
			.value(root.get(TOUCHED_ATTRIBUTE))
			.value(root.get(MODIFIED_ATTRIBUTE))
			.value(root.get(CREATED_ATTRIBUTE)), cutoff);
	}

	/**
	 * Matches errands whose id sorts after the sent in one, which lets a walk over a namespace carry on from where the
	 * previous batch ended.
	 *
	 * @param  id the id the previous batch ended on
	 * @return    specification matching errands that come after the sent in id
	 */
	public static Specification<ErrandEntity> withIdAfter(String id) {
		return (root, _, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get(ID_ATTRIBUTE), id);
	}

	/**
	 * Matches errands reported by sent in user. A null user matches nothing, not even errands lacking a reporter.
	 *
	 * @param  adAccount ad account of the requesting user, or null
	 * @return           specification matching errands reported by sent in user
	 */
	public static Specification<ErrandEntity> isReportedBy(String adAccount) {
		return (root, _, criteriaBuilder) -> isNull(adAccount)
			? criteriaBuilder.disjunction()
			: criteriaBuilder.equal(root.get(REPORTER_USER_ID_ATTRIBUTE), adAccount);
	}

	/**
	 * Matches errands whose every access label is among sent in allowed labels.
	 * <p>
	 * Expressed as "has no access label outside the allowed set", in one correlated subquery. An errand carrying no
	 * access labels has nothing outside the set and is matched for any non-empty set of allowed labels.
	 *
	 * @param  allowedLabels labels the user may see, no access at all if empty
	 * @return               specification matching errands fully covered by sent in labels
	 */
	public static Specification<ErrandEntity> hasAllowedMetadataLabels(Set<MetadataLabelEntity> allowedLabels) {
		return (root, query, criteriaBuilder) -> {
			if (allowedLabels == null || allowedLabels.isEmpty()) {
				return criteriaBuilder.disjunction(); // No access if no allowed labels
			}

			final var allowedLabelIds = allowedLabels.stream()
				.map(MetadataLabelEntity::getId)
				.collect(Collectors.toSet());

			final Subquery<Integer> labelsOutsideAllowed = query.subquery(Integer.class);
			final Root<ErrandEntity> subRoot = labelsOutsideAllowed.from(ErrandEntity.class);
			final Join<ErrandEntity, AccessLabelEmbeddable> labelJoin = subRoot.join(ACCESS_LABELS_ATTRIBUTE, JoinType.INNER);

			labelsOutsideAllowed.select(criteriaBuilder.literal(1))
				.where(
					criteriaBuilder.equal(subRoot.get(ID_ATTRIBUTE), root.get(ID_ATTRIBUTE)),
					labelJoin.get(METADATA_LABEL_ID_ATTRIBUTE).in(allowedLabelIds).not());

			return criteriaBuilder.not(criteriaBuilder.exists(labelsOutsideAllowed));
		};
	}

	/**
	 * Method builds an equal filter if value is not null. If value is null, method returns an always-true predicate
	 * (meaning no filtering will be applied for sent in attribute)
	 *
	 * @param  attribute name that will be used in filter
	 * @param  value     value (or null) to compare against
	 * @return           Specification<T> matching sent in comparison
	 */
	private Specification<T> buildEqualFilter(String attribute, Object value) {
		return (entity, _, cb) -> nonNull(value) ? cb.equal(entity.get(attribute), value) : cb.and();
	}

	/**
	 * Whether the parsed filter names the sent in field, or a path beneath it, anywhere in its tree.
	 */
	private static boolean namesField(FilterNode node, String field) {
		if (isNull(node)) {
			return false;
		}

		if (node instanceof final FieldNode fieldNode && (field.equals(fieldNode.getName()) || Strings.CS.startsWith(fieldNode.getName(), field + "."))) {
			return true;
		}

		return Optional.ofNullable(node.getChildren()).orElse(List.of()).stream()
			.anyMatch(child -> namesField(child, field));
	}
}
