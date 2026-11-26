package se.sundsvall.supportmanagement.integration.db.specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.model.enums.JobStatus;
import se.sundsvall.supportmanagement.model.enums.JobType;

import static java.util.Optional.ofNullable;

public interface JobSpecification {

	/**
	 * The jobs of a namespace, narrowed to a status and a kind where those are sent in.
	 *
	 * @param  namespace      namespace the jobs belong to.
	 * @param  municipalityId id of the municipality the jobs belong to.
	 * @param  status         the status, or {@code null} for every status.
	 * @param  type           the kind, or {@code null} for every kind.
	 * @return                the specification.
	 */
	static Specification<JobEntity> within(final String namespace, final String municipalityId, final JobStatus status, final JobType type) {
		return (root, _, builder) -> {
			final var predicates = new ArrayList<Predicate>();
			predicates.add(builder.equal(root.get("namespace"), namespace));
			predicates.add(builder.equal(root.get("municipalityId"), municipalityId));
			ofNullable(status).ifPresent(value -> predicates.add(builder.equal(root.get("status"), value)));
			ofNullable(type).ifPresent(value -> predicates.add(builder.equal(root.get("type"), value)));
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
