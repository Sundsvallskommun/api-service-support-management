package se.sundsvall.supportmanagement.integration.db.specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

public interface ErrandSpecification {

	static Specification<ErrandEntity> hasMatchingTags(final List<DbExternalTag> tags) {
		return (root, query, criteriaBuilder) -> {
			if (tags == null || tags.isEmpty()) {
				return criteriaBuilder.conjunction();
			}

			final List<Predicate> predicates = new ArrayList<>();
			for (final DbExternalTag tag : tags) {
				predicates.add(criteriaBuilder.and(
					criteriaBuilder.equal(root.join("externalTags").get("key"), tag.getKey()),
					criteriaBuilder.equal(root.join("externalTags").get("value"), tag.getValue())
				));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
