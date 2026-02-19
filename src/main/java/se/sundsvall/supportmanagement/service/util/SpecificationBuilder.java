package se.sundsvall.supportmanagement.service.util;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Objects.nonNull;

public class SpecificationBuilder<T> {

	private static final SpecificationBuilder<ErrandEntity> ERRAND_ENTITY_BUILDER = new SpecificationBuilder<>();
	private static final String LABELS_ATTRIBUTE = "labels";
	private static final String ID_ATTRIBUTE = "id";
	private static final String METADATA_LABEL_ID_ATTRIBUTE = "metadataLabelId";

	public static Specification<ErrandEntity> withNamespace(String namespace) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("namespace", namespace);
	}

	public static Specification<ErrandEntity> withMunicipalityId(String municipalityId) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("municipalityId", municipalityId);
	}

	public static Specification<ErrandEntity> withId(String id) {
		return ERRAND_ENTITY_BUILDER.buildEqualFilter("id", id);
	}

	public static Specification<ErrandEntity> hasAllowedMetadataLabels(Set<MetadataLabelEntity> allowedLabels) {
		return (root, query, criteriaBuilder) -> {
			if (allowedLabels == null || allowedLabels.isEmpty()) {
				return criteriaBuilder.disjunction(); // No access if no allowed labels
			}

			// Extract IDs from the MetadataLabelEntity set
			Set<String> allowedLabelIds = allowedLabels.stream()
				.map(MetadataLabelEntity::getId)
				.collect(Collectors.toSet());

			// Subquery 1: Count total labels for this errand
			Subquery<Long> totalLabelsSubquery = query.subquery(Long.class);
			Root<ErrandEntity> totalRoot = totalLabelsSubquery.from(ErrandEntity.class);
			Join<ErrandEntity, ErrandLabelEmbeddable> totalLabelJoin = totalRoot.join(LABELS_ATTRIBUTE, JoinType.LEFT);

			totalLabelsSubquery.select(criteriaBuilder.count(totalLabelJoin))
				.where(criteriaBuilder.equal(totalRoot.get(ID_ATTRIBUTE), root.get(ID_ATTRIBUTE)));

			// Subquery 2: Count labels that are in the allowed list
			Subquery<Long> allowedLabelsSubquery = query.subquery(Long.class);
			Root<ErrandEntity> allowedRoot = allowedLabelsSubquery.from(ErrandEntity.class);
			Join<ErrandEntity, ErrandLabelEmbeddable> allowedLabelJoin = allowedRoot.join(LABELS_ATTRIBUTE, JoinType.LEFT);

			allowedLabelsSubquery.select(criteriaBuilder.count(allowedLabelJoin))
				.where(
					criteriaBuilder.equal(allowedRoot.get(ID_ATTRIBUTE), root.get(ID_ATTRIBUTE)),
					allowedLabelJoin.get(METADATA_LABEL_ID_ATTRIBUTE).in(allowedLabelIds));

			// Check if counts are equal
			// For errands with no labels: 0 == 0 → TRUE → accessible to everyone
			return criteriaBuilder.equal(totalLabelsSubquery, allowedLabelsSubquery);
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
		return (entity, cq, cb) -> nonNull(value) ? cb.equal(entity.get(attribute), value) : cb.and();
	}

}
