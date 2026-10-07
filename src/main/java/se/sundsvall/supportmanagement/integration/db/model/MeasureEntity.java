package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import se.sundsvall.supportmanagement.integration.db.model.enums.Accept;
import se.sundsvall.supportmanagement.integration.db.model.enums.MeasureResult;
import se.sundsvall.supportmanagement.integration.db.search.JsonParametersBinder;
import se.sundsvall.supportmanagement.integration.db.search.OffsetDateTimeBinder;

import static jakarta.persistence.CascadeType.ALL;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static se.sundsvall.supportmanagement.integration.db.search.SearchAnalysisConfigurer.LOWERCASE;
import static se.sundsvall.supportmanagement.integration.db.search.SearchAnalysisConfigurer.TEXT;

/**
 * A measure on an errand.
 * <p>
 * The oldest of the four handling artefacts, and the reason the other three were shaped after it rather than the other
 * way around. It carries fields inherited from the action plan flow of a single line of business - {@code goal},
 * {@code accept} and {@code acceptMotivation}. A new line of business must not fill them with anything other than what
 * they mean. Where an accept step is needed, it belongs in a JSON parameter.
 * <p>
 * The rework fields of that same flow were dropped rather than carried forward, having never been filled by anything.
 * <p>
 * Two inherited fields are overridden rather than migrated. {@code type} is 255 characters here and 128 in the base
 * class, and {@code description} is 3000 rather than a long text: narrowing a column that already holds data would
 * truncate it in silence, and a long text buys nothing over the length the goal and the description were widened to.
 * <p>
 * Rows that predate the shared shape had their municipality and namespace filled from their errand, and their status
 * read from {@code executed}: {@code COMPLETED} where it was set, {@code ACTIVE} otherwise. The foreign key to the
 * errand cascades in the database as it does for the other three artefacts, although the errand still holds measures
 * as a collection - a removal that bypasses the persistence context must not fail on the one table that differs.
 */
@Entity
@Table(name = "measure",
	indexes = {
		@Index(name = "idx_measure_errand_id", columnList = "errand_id"),
		@Index(name = "idx_measure_ns_status", columnList = "municipality_id,namespace,status"),
		@Index(name = "idx_measure_due_at", columnList = "due_at")
	})
@AssociationOverride(name = "errandEntity",
	joinColumns = @JoinColumn(name = "errand_id", nullable = false),
	foreignKey = @ForeignKey(name = "fk_measure_errand_id"))
@AttributeOverride(name = "type", column = @Column(name = "type", length = 255))
@AttributeOverride(name = "description", column = @Column(name = "description", length = 3000))
public class MeasureEntity extends AbstractErrandItemEntity<MeasureEntity> {

	@Column(name = "responsible_user")
	@KeywordField(normalizer = LOWERCASE)
	private String responsibleUser;

	@Column(name = "planned_start")
	@TimeZoneStorage(NORMALIZE)
	@NonStandardField(valueBinder = @ValueBinderRef(type = OffsetDateTimeBinder.class))
	private OffsetDateTime plannedStart;

	@Column(name = "planned_complete")
	@TimeZoneStorage(NORMALIZE)
	@NonStandardField(valueBinder = @ValueBinderRef(type = OffsetDateTimeBinder.class))
	private OffsetDateTime plannedComplete;

	@Column(name = "executed")
	@TimeZoneStorage(NORMALIZE)
	@NonStandardField(valueBinder = @ValueBinderRef(type = OffsetDateTimeBinder.class))
	private OffsetDateTime executed;

	@Column(name = "added_by_user")
	@KeywordField(normalizer = LOWERCASE)
	private String addedByUser;

	@Column(name = "added_by_role")
	@KeywordField(normalizer = LOWERCASE)
	private String addedByRole;

	@Column(name = "goal", length = 3000)
	@FullTextField(analyzer = TEXT)
	private String goal;

	// The length says what the column has held since V1_51, and the jdbc type says it is a string rather than the
	// native enum type Hibernate would otherwise generate for it.
	@Column(name = "accept", length = 50)
	@Enumerated(EnumType.STRING)
	@KeywordField(normalizer = LOWERCASE)
	@JdbcTypeCode(VARCHAR)
	private Accept accept;

	@Column(name = "accept_motivation")
	@FullTextField(analyzer = TEXT)
	private String acceptMotivation;

	/** The outcome once the measure has been carried out. Not {@link Accept}, which means something else. */
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(VARCHAR)
	@Column(name = "result", length = 32)
	@KeywordField(normalizer = LOWERCASE)
	private MeasureResult result;

	@Column(name = "result_text", length = LONG32)
	@FullTextField(analyzer = TEXT)
	private String resultText;

	/** Where the measure comes from: it follows from a decision. Set to null rather than cascading. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "decision_id", foreignKey = @ForeignKey(name = "fk_measure_decision_id"))
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private DecisionEntity decisionEntity;

	/** Or from the response to a statement. Set to null rather than cascading. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "statement_id", foreignKey = @ForeignKey(name = "fk_measure_statement_id"))
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private StatementEntity statementEntity;

	/**
	 * The attachments of the errand this measure uses. See {@link StatementEntity#getAttachments()} for how they are held.
	 */
	@ManyToMany
	@JoinTable(name = "measure_attachment",
		joinColumns = @JoinColumn(name = "measure_id"),
		inverseJoinColumns = @JoinColumn(name = "attachment_id"),
		foreignKey = @ForeignKey(name = "fk_measure_attachment_measure_id", options = "on delete cascade"),
		inverseForeignKey = @ForeignKey(name = "fk_measure_attachment_attachment_id", options = "on delete cascade"),
		uniqueConstraints = @UniqueConstraint(name = "uq_measure_attachment_measure_id_attachment_id", columnNames = {
			"measure_id", "attachment_id"
		}),
		indexes = {
			@Index(name = "idx_measure_attachment_measure_id", columnList = "measure_id"),
			@Index(name = "idx_measure_attachment_attachment_id", columnList = "attachment_id")
		})
	@OrderBy("fileName")
	private List<AttachmentEntity> attachments;

	/** The JSON parameters of the measure. See {@link StatementEntity#getJsonParameters()} for how they are held. */
	@OneToMany(mappedBy = "measureEntity", cascade = ALL, orphanRemoval = true)
	@OrderBy("key")
	@PropertyBinding(binder = @PropertyBinderRef(type = JsonParametersBinder.class))
	private List<MeasureJsonParameterEntity> jsonParameters;

	public static MeasureEntity create() {
		return new MeasureEntity();
	}

	public String getResponsibleUser() {
		return responsibleUser;
	}

	public void setResponsibleUser(final String responsibleUser) {
		this.responsibleUser = responsibleUser;
	}

	public MeasureEntity withResponsibleUser(final String responsibleUser) {
		this.responsibleUser = responsibleUser;
		return this;
	}

	public OffsetDateTime getPlannedStart() {
		return plannedStart;
	}

	public void setPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
	}

	public MeasureEntity withPlannedStart(final OffsetDateTime plannedStart) {
		this.plannedStart = plannedStart;
		return this;
	}

	public OffsetDateTime getPlannedComplete() {
		return plannedComplete;
	}

	public void setPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
	}

	public MeasureEntity withPlannedComplete(final OffsetDateTime plannedComplete) {
		this.plannedComplete = plannedComplete;
		return this;
	}

	public OffsetDateTime getExecuted() {
		return executed;
	}

	public void setExecuted(final OffsetDateTime executed) {
		this.executed = executed;
	}

	public MeasureEntity withExecuted(final OffsetDateTime executed) {
		this.executed = executed;
		return this;
	}

	public String getAddedByUser() {
		return addedByUser;
	}

	public void setAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
	}

	public MeasureEntity withAddedByUser(final String addedByUser) {
		this.addedByUser = addedByUser;
		return this;
	}

	public String getAddedByRole() {
		return addedByRole;
	}

	public void setAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
	}

	public MeasureEntity withAddedByRole(final String addedByRole) {
		this.addedByRole = addedByRole;
		return this;
	}

	public String getGoal() {
		return goal;
	}

	public void setGoal(final String goal) {
		this.goal = goal;
	}

	public MeasureEntity withGoal(final String goal) {
		this.goal = goal;
		return this;
	}

	public Accept getAccept() {
		return accept;
	}

	public void setAccept(final Accept accept) {
		this.accept = accept;
	}

	public MeasureEntity withAccept(final Accept accept) {
		this.accept = accept;
		return this;
	}

	public String getAcceptMotivation() {
		return acceptMotivation;
	}

	public void setAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
	}

	public MeasureEntity withAcceptMotivation(final String acceptMotivation) {
		this.acceptMotivation = acceptMotivation;
		return this;
	}

	public MeasureResult getResult() {
		return result;
	}

	public void setResult(final MeasureResult result) {
		this.result = result;
	}

	public MeasureEntity withResult(final MeasureResult result) {
		this.result = result;
		return this;
	}

	public String getResultText() {
		return resultText;
	}

	public void setResultText(final String resultText) {
		this.resultText = resultText;
	}

	public MeasureEntity withResultText(final String resultText) {
		this.resultText = resultText;
		return this;
	}

	public DecisionEntity getDecisionEntity() {
		return decisionEntity;
	}

	public void setDecisionEntity(final DecisionEntity decisionEntity) {
		this.decisionEntity = decisionEntity;
	}

	public MeasureEntity withDecisionEntity(final DecisionEntity decisionEntity) {
		this.decisionEntity = decisionEntity;
		return this;
	}

	public StatementEntity getStatementEntity() {
		return statementEntity;
	}

	public void setStatementEntity(final StatementEntity statementEntity) {
		this.statementEntity = statementEntity;
	}

	public MeasureEntity withStatementEntity(final StatementEntity statementEntity) {
		this.statementEntity = statementEntity;
		return this;
	}

	public List<AttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<AttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public MeasureEntity withAttachments(final List<AttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<MeasureJsonParameterEntity> getJsonParameters() {
		return jsonParameters;
	}

	public void setJsonParameters(final List<MeasureJsonParameterEntity> jsonParameters) {
		this.jsonParameters = jsonParameters;
	}

	public MeasureEntity withJsonParameters(final List<MeasureJsonParameterEntity> jsonParameters) {
		this.jsonParameters = jsonParameters;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final MeasureEntity that = (MeasureEntity) o;
		return Objects.equals(responsibleUser, that.responsibleUser)
			&& Objects.equals(plannedStart, that.plannedStart)
			&& Objects.equals(plannedComplete, that.plannedComplete)
			&& Objects.equals(executed, that.executed)
			&& Objects.equals(addedByUser, that.addedByUser)
			&& Objects.equals(addedByRole, that.addedByRole)
			&& Objects.equals(goal, that.goal)
			&& (accept == that.accept)
			&& Objects.equals(acceptMotivation, that.acceptMotivation)
			&& (result == that.result)
			&& Objects.equals(resultText, that.resultText);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), responsibleUser, plannedStart, plannedComplete, executed, addedByUser, addedByRole, goal, accept, acceptMotivation, result,
			resultText);
	}

	@Override
	public String toString() {
		return "MeasureEntity{" + super.toString() +
			", responsibleUser='" + responsibleUser + '\'' +
			", plannedStart=" + plannedStart +
			", plannedComplete=" + plannedComplete +
			", executed=" + executed +
			", addedByUser='" + addedByUser + '\'' +
			", addedByRole='" + addedByRole + '\'' +
			", goal='" + goal + '\'' +
			", accept=" + accept +
			", acceptMotivation='" + acceptMotivation + '\'' +
			", result=" + result +
			", resultText='" + resultText + '\'' +
			", decisionEntity=" + (decisionEntity != null ? decisionEntity.getId() : "null") +
			", statementEntity=" + (statementEntity != null ? statementEntity.getId() : "null") +
			'}';
	}
}
