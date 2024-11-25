package se.sundsvall.supportmanagement.integration.db.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import java.util.ArrayList;
import java.util.List;

public class ErrandSpecification {

	public static Specification<ErrandEntity> hasMatchingTags(List<DbExternalTag> tags) {
		return (root, query, criteriaBuilder) -> {
			if (tags == null || tags.isEmpty()) {
				return criteriaBuilder.conjunction();
			}

			List<Predicate> predicates = new ArrayList<>();
			for (DbExternalTag tag : tags) {
				predicates.add(criteriaBuilder.and(
					criteriaBuilder.equal(root.join("externalTags").get("key"), tag.getKey()),
					criteriaBuilder.equal(root.join("externalTags").get("value"), tag.getValue())
				));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
