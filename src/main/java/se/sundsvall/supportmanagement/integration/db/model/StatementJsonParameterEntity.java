package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import static jakarta.persistence.FetchType.LAZY;

/**
 * A JSON parameter of a statement. See {@link AbstractArtefactJsonParameterEntity} for why it is not one of the errand.
 */
@Entity
@Table(name = "statement_json_parameter",
	uniqueConstraints = @UniqueConstraint(name = "uq_statement_json_parameter_statement_id_key", columnNames = {
		"statement_id", "parameter_key"
	}))
public class StatementJsonParameterEntity extends AbstractArtefactJsonParameterEntity<StatementJsonParameterEntity> {

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "statement_id", nullable = false, foreignKey = @ForeignKey(name = "fk_statement_json_parameter_statement_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private StatementEntity statementEntity;

	public static StatementJsonParameterEntity create() {
		return new StatementJsonParameterEntity();
	}

	public StatementEntity getStatementEntity() {
		return statementEntity;
	}

	public void setStatementEntity(final StatementEntity statementEntity) {
		this.statementEntity = statementEntity;
	}

	public StatementJsonParameterEntity withStatementEntity(final StatementEntity statementEntity) {
		this.statementEntity = statementEntity;
		return this;
	}

	@Override
	public String toString() {
		return "StatementJsonParameterEntity{" + super.toString() +
			", statementEntity=" + (statementEntity != null ? statementEntity.getId() : "null") +
			'}';
	}
}
