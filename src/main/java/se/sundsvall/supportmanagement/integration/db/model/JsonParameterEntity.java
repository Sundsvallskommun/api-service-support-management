package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;

@Entity
@Table(name = "json_parameter",
	indexes = {
		@Index(name = "idx_json_parameter_errand_id", columnList = "errand_id"),
		@Index(name = "idx_json_parameter_key", columnList = "parameter_key")
	},
	uniqueConstraints = @UniqueConstraint(name = "uq_json_parameter_errand_id_key", columnNames = {
		"errand_id", "parameter_key"
	}))
public class JsonParameterEntity {

	@Id
	@UuidGenerator
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_json_parameter_errand_id"))
	private ErrandEntity errandEntity;

	@Column(name = "parameter_key")
	private String key;

	@Version
	@Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
	private Long version;

	@Column(name = "schema_id")
	private String schemaId;

	@JdbcTypeCode(LONG32VARCHAR)
	@Column(name = "value", columnDefinition = "longtext")
	private String value;

	// The link to the handling artefact this parameter is the content of, at most one across the five. cascade = REMOVE
	// and nothing else, as on the attachment: the parameter tears its link down before it goes itself. Without it a
	// parameter removed in the same flush as the measure owning it goes first - the errand cascades its parameters before
	// its measures - and Hibernate then nulls the reference of the link to it before deleting the link, which the column
	// refuses. Batched, since a patch of the errand asks each of its parameters whether an artefact owns it.
	@BatchSize(size = 50)
	@OneToMany(mappedBy = "jsonParameterEntity", cascade = REMOVE)
	private List<StatementJsonParameterEntity> statementLinks;

	@BatchSize(size = 50)
	@OneToMany(mappedBy = "jsonParameterEntity", cascade = REMOVE)
	private List<InvestigationJsonParameterEntity> investigationLinks;

	@BatchSize(size = 50)
	@OneToMany(mappedBy = "jsonParameterEntity", cascade = REMOVE)
	private List<InvestigationSectionJsonParameterEntity> investigationSectionLinks;

	@BatchSize(size = 50)
	@OneToMany(mappedBy = "jsonParameterEntity", cascade = REMOVE)
	private List<DecisionJsonParameterEntity> decisionLinks;

	@BatchSize(size = 50)
	@OneToMany(mappedBy = "jsonParameterEntity", cascade = REMOVE)
	private List<MeasureJsonParameterEntity> measureLinks;

	public static JsonParameterEntity create() {
		return new JsonParameterEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public JsonParameterEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public JsonParameterEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public JsonParameterEntity withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getSchemaId() {
		return schemaId;
	}

	public void setSchemaId(final String schemaId) {
		this.schemaId = schemaId;
	}

	public JsonParameterEntity withSchemaId(final String schemaId) {
		this.schemaId = schemaId;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public JsonParameterEntity withValue(final String value) {
		this.value = value;
		return this;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public JsonParameterEntity withVersion(final Long version) {
		this.version = version;
		return this;
	}

	public List<StatementJsonParameterEntity> getStatementLinks() {
		return statementLinks;
	}

	public void setStatementLinks(final List<StatementJsonParameterEntity> statementLinks) {
		this.statementLinks = statementLinks;
	}

	public JsonParameterEntity withStatementLinks(final List<StatementJsonParameterEntity> statementLinks) {
		this.statementLinks = statementLinks;
		return this;
	}

	public List<InvestigationJsonParameterEntity> getInvestigationLinks() {
		return investigationLinks;
	}

	public void setInvestigationLinks(final List<InvestigationJsonParameterEntity> investigationLinks) {
		this.investigationLinks = investigationLinks;
	}

	public JsonParameterEntity withInvestigationLinks(final List<InvestigationJsonParameterEntity> investigationLinks) {
		this.investigationLinks = investigationLinks;
		return this;
	}

	public List<InvestigationSectionJsonParameterEntity> getInvestigationSectionLinks() {
		return investigationSectionLinks;
	}

	public void setInvestigationSectionLinks(final List<InvestigationSectionJsonParameterEntity> investigationSectionLinks) {
		this.investigationSectionLinks = investigationSectionLinks;
	}

	public JsonParameterEntity withInvestigationSectionLinks(final List<InvestigationSectionJsonParameterEntity> investigationSectionLinks) {
		this.investigationSectionLinks = investigationSectionLinks;
		return this;
	}

	public List<DecisionJsonParameterEntity> getDecisionLinks() {
		return decisionLinks;
	}

	public void setDecisionLinks(final List<DecisionJsonParameterEntity> decisionLinks) {
		this.decisionLinks = decisionLinks;
	}

	public JsonParameterEntity withDecisionLinks(final List<DecisionJsonParameterEntity> decisionLinks) {
		this.decisionLinks = decisionLinks;
		return this;
	}

	public List<MeasureJsonParameterEntity> getMeasureLinks() {
		return measureLinks;
	}

	public void setMeasureLinks(final List<MeasureJsonParameterEntity> measureLinks) {
		this.measureLinks = measureLinks;
	}

	public JsonParameterEntity withMeasureLinks(final List<MeasureJsonParameterEntity> measureLinks) {
		this.measureLinks = measureLinks;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, key, schemaId, value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final JsonParameterEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(key, other.key) && Objects.equals(schemaId, other.schemaId) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "JsonParameterEntity{" +
			"id='" + id + '\'' +
			", key='" + key + '\'' +
			", version=" + version +
			", schemaId='" + schemaId + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
