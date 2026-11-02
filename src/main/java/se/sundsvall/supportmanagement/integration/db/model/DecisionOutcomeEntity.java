package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The outcomes a decision may have, registered per namespace.
 * <p>
 * Nothing is seeded: each namespace registers the outcomes it uses. The code decides nothing on the name of an outcome.
 * The recommendation of an investigation is held to the same outcomes as the decision it proposes.
 */
@Entity
@Table(name = "decision_outcome",
	indexes = {
		@Index(name = "idx_decision_outcome_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_decision_outcome_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class DecisionOutcomeEntity extends AbstractNamedMetadataEntity<DecisionOutcomeEntity> {

	public static DecisionOutcomeEntity create() {
		return new DecisionOutcomeEntity();
	}
}
