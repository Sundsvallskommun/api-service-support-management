package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

/**
 * The outcomes the response to a statement may have, registered per namespace.
 * <p>
 * Nothing is seeded. {@link #isResponded()} says whether the outcome means that the counterparty responded, which
 * decides whether a statement completed with it needs the time of the response.
 */
@Entity
@Table(name = "statement_outcome",
	indexes = {
		@Index(name = "idx_statement_outcome_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_statement_outcome_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class StatementOutcomeEntity extends AbstractNamedMetadataEntity<StatementOutcomeEntity> {

	/** False for an outcome such as the deadline passing without a response. */
	@Column(name = "responded", nullable = false)
	private boolean responded;

	public static StatementOutcomeEntity create() {
		return new StatementOutcomeEntity();
	}

	public boolean isResponded() {
		return responded;
	}

	public void setResponded(final boolean responded) {
		this.responded = responded;
	}

	public StatementOutcomeEntity withResponded(final boolean responded) {
		this.responded = responded;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		return super.equals(o) && (responded == ((StatementOutcomeEntity) o).responded);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), responded);
	}

	@Override
	protected String fieldsToString() {
		return super.fieldsToString() + ", responded=" + responded;
	}
}
