package se.sundsvall.supportmanagement.service.util;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static java.util.Objects.nonNull;

public class SpecificationBuilder<T> {

	private static final SpecificationBuilder<ErrandEntity> ERRAND_ENTITY_BUILDER = new SpecificationBuilder<>();
	private static final String ID_ATTRIBUTE = "id";
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

	/**
	 * Matches errands that have not been touched since the sent in point in time.
	 * <p>
	 * Which timestamp says when an errand was last touched depends on what has happened to it, so the first one that is
	 * set decides. An errand carrying none of them is left out: one that cannot be dated cannot be shown to be old
	 * enough to act on, and the coalesce answers null for it.
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
	 * Matches errands whose id sorts after the sent in one, which is how a walk over a namespace carries on from where
	 * the previous batch ended without stepping over what moved up behind a removed errand.
	 *
	 * @param  id the id the previous batch ended on
	 * @return    specification matching errands that come after the sent in id
	 */
	public static Specification<ErrandEntity> withIdAfter(String id) {
		return (root, _, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get(ID_ATTRIBUTE), id);
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
