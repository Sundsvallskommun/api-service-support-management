package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.FetchType.LAZY;

/**
 * Links a JSON parameter of the errand to a statement.
 * <p>
 * The parameter row itself stays owned by {@code ErrandEntity.jsonParameters}, and that is what removes it. Neither
 * association here cascades: a link that carried a removal onwards would take a parameter out from under a collection
 * that still holds it, and the next flush would write it back.
 * <p>
 * The unique key on the parameter alone is what makes the ownership single. Without it two artefacts could claim the
 * same parameter, and removing either would take content the other still shows.
 */
@Entity
@Table(name = "statement_json_parameter",
	indexes = {
		@Index(name = "idx_statement_json_parameter_statement_id", columnList = "statement_id"),
		@Index(name = "idx_statement_json_parameter_json_parameter_id", columnList = "json_parameter_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_statement_json_parameter_statement_id_json_parameter_id", columnNames = {
			"statement_id", "json_parameter_id"
		}),
		@UniqueConstraint(name = "uq_statement_json_parameter_json_parameter_id", columnNames = {
			"json_parameter_id"
		})
	})
public class StatementJsonParameterEntity implements JsonParameterLink {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "statement_id", nullable = false, foreignKey = @ForeignKey(name = "fk_statement_json_parameter_statement_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private StatementEntity statementEntity;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "json_parameter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_statement_json_parameter_json_parameter_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private JsonParameterEntity jsonParameterEntity;

	public static StatementJsonParameterEntity create() {
		return new StatementJsonParameterEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public StatementJsonParameterEntity withId(final String id) {
		this.id = id;
		return this;
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
	public JsonParameterEntity getJsonParameterEntity() {
		return jsonParameterEntity;
	}

	public void setJsonParameterEntity(final JsonParameterEntity jsonParameterEntity) {
		this.jsonParameterEntity = jsonParameterEntity;
	}

	public StatementJsonParameterEntity withJsonParameterEntity(final JsonParameterEntity jsonParameterEntity) {
		this.jsonParameterEntity = jsonParameterEntity;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final StatementJsonParameterEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "StatementJsonParameterEntity{" +
			"id='" + id + '\'' +
			", statementEntity=" + (statementEntity != null ? statementEntity.getId() : "null") +
			", jsonParameterEntity=" + (jsonParameterEntity != null ? jsonParameterEntity.getId() : "null") +
			'}';
	}
}
