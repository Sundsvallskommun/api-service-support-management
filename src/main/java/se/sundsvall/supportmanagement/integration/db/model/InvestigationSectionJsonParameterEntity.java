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
 * Links a JSON parameter of the errand to an investigation section.
 * <p>
 * The parameter row itself stays owned by {@code ErrandEntity.jsonParameters}, and that is what removes it. Neither
 * association here cascades: a link that carried a removal onwards would take a parameter out from under a collection
 * that still holds it, and the next flush would write it back.
 * <p>
 * The unique key on the parameter alone is what makes the ownership single. Without it two artefacts could claim the
 * same parameter, and removing either would take content the other still shows.
 */
@Entity
@Table(name = "investigation_section_json_parameter",
	indexes = {
		@Index(name = "idx_investigation_section_json_parameter_section_id", columnList = "investigation_section_id"),
		@Index(name = "idx_investigation_section_json_parameter_parameter_id", columnList = "json_parameter_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_investigation_section_json_parameter_section_parameter", columnNames = {
			"investigation_section_id", "json_parameter_id"
		}),
		@UniqueConstraint(name = "uq_investigation_section_json_parameter_parameter_id", columnNames = {
			"json_parameter_id"
		})
	})
public class InvestigationSectionJsonParameterEntity implements JsonParameterLink {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "investigation_section_id", nullable = false, foreignKey = @ForeignKey(name = "fk_investigation_section_json_parameter_section_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private InvestigationSectionEntity investigationSectionEntity;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "json_parameter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_investigation_section_json_parameter_parameter_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private JsonParameterEntity jsonParameterEntity;

	public static InvestigationSectionJsonParameterEntity create() {
		return new InvestigationSectionJsonParameterEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public InvestigationSectionJsonParameterEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public InvestigationSectionEntity getInvestigationSectionEntity() {
		return investigationSectionEntity;
	}

	public void setInvestigationSectionEntity(final InvestigationSectionEntity investigationSectionEntity) {
		this.investigationSectionEntity = investigationSectionEntity;
	}

	public InvestigationSectionJsonParameterEntity withInvestigationSectionEntity(final InvestigationSectionEntity investigationSectionEntity) {
		this.investigationSectionEntity = investigationSectionEntity;
		return this;
	}

	@Override
	public JsonParameterEntity getJsonParameterEntity() {
		return jsonParameterEntity;
	}

	public void setJsonParameterEntity(final JsonParameterEntity jsonParameterEntity) {
		this.jsonParameterEntity = jsonParameterEntity;
	}

	public InvestigationSectionJsonParameterEntity withJsonParameterEntity(final JsonParameterEntity jsonParameterEntity) {
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
		if (!(obj instanceof final InvestigationSectionJsonParameterEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "InvestigationSectionJsonParameterEntity{" +
			"id='" + id + '\'' +
			", investigationSectionEntity=" + (investigationSectionEntity != null ? investigationSectionEntity.getId() : "null") +
			", jsonParameterEntity=" + (jsonParameterEntity != null ? jsonParameterEntity.getId() : "null") +
			'}';
	}
}
