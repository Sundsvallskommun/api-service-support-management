package se.sundsvall.supportmanagement.service.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Objects.nonNull;

public class SpecificationBuilder<T> {

	private static final SpecificationBuilder<ErrandEntity> ERRAND_ENTITY_BUILDER = new SpecificationBuilder<>();
	private static final String ACCESS_LABELS_ATTRIBUTE = "accessLabels";
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

	public static Specification<ErrandEntity> withIdIn(List<String> ids) {
		return (root, _, _) -> root.get(ID_ATTRIBUTE).in(ids);
	}

	public static Specification<ErrandEntity> hasAllowedMetadataLabels(Set<MetadataLabelEntity> allowedLabels) {
		return (root, query, criteriaBuilder) -> {
			if (allowedLabels == null || allowedLabels.isEmpty()) {
				return criteriaBuilder.disjunction(); // No access if no allowed labels
			}

			final var allowedLabelIds = allowedLabels.stream()
				.map(MetadataLabelEntity::getId)
				.collect(Collectors.toSet());

			// Accessible if no access label exists that is NOT in the allowed set.
			// For errands with no labels: NOT EXISTS → TRUE → accessible to everyone.
			final var subquery = query.subquery(Integer.class);
			final var subRoot = subquery.from(ErrandEntity.class);
			final var labelJoin = subRoot.join(ACCESS_LABELS_ATTRIBUTE);

			subquery.select(criteriaBuilder.literal(1))
				.where(
					criteriaBuilder.equal(subRoot.get(ID_ATTRIBUTE), root.get(ID_ATTRIBUTE)),
					criteriaBuilder.not(labelJoin.get(METADATA_LABEL_ID_ATTRIBUTE).in(allowedLabelIds)));

			return criteriaBuilder.not(criteriaBuilder.exists(subquery));
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
