package se.sundsvall.supportmanagement.service.util;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class SpecificationBuilder<T> {

	private static final SpecificationBuilder<ErrandEntity> ERRAND_ENTITY_BUILDER = new SpecificationBuilder<>();
	private static final String ACCESS_LABELS_ATTRIBUTE = "accessLabels";
	private static final String ID_ATTRIBUTE = "id";
	private static final String METADATA_LABEL_ID_ATTRIBUTE = "metadataLabelId";
	private static final String REPORTER_USER_ID_ATTRIBUTE = "reporterUserId";

	public static Specification<ErrandEntity> withNamespace(String namespace) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("namespace", namespace);
	}

	public static Specification<ErrandEntity> withMunicipalityId(String municipalityId) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("municipalityId", municipalityId);
	}

	public static Specification<ErrandEntity> withId(String id) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("id", id);
	}

	/**
	 * Matches errands whose id is among sent in ids.
	 *
	 * @param  ids ids to match against
	 * @return     specification matching errands with any of sent in ids
	 */
	public static Specification<ErrandEntity> withIds(Collection<String> ids) {
		return (root, _, _) -> root.get(ID_ATTRIBUTE).in(ids);
	}

	/**
	 * Matches errands reported by sent in user. A null user matches nothing, so an absent or non AD identifier can never
	 * match errands lacking a reporter.
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
	 * Expressed as "has no access label outside the allowed set" rather than by counting labels and matching labels and
	 * comparing the two. One correlated subquery instead of two aggregating ones, and it stops at the first offending
	 * label rather than counting every row. An errand carrying no access labels has nothing outside the set and stays
	 * accessible to everyone, which is the same rule as the counting form giving 0 == 0.
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
}
