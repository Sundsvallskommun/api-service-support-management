package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * One assessed area of an investigation.
 * <p>
 * The assessment is the only structured thing here, and therefore the only thing that can be counted across errands.
 * Whatever needs more structure than free text goes in a JSON parameter on the section.
 */
@Schema(description = "Investigation section model")
public class InvestigationSection {

	@Schema(description = "Section ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Key of the section, stable over time. Without whitespace", maxLength = 64, examples = "financial")
	@NotBlank(groups = {
		Default.class, OnCreate.class
	})
	@Size(max = 64, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	@Pattern(regexp = "\\S+", message = "must not be blank or contain whitespace", groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String sectionKey;

	@Schema(description = "Heading shown for the section", maxLength = 255, examples = "Ekonomisk skötsamhet")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String heading;

	@Schema(description = "Order the section is shown in", examples = "2")
	private Integer sortOrder;

	@Schema(description = "Assessment of the section", examples = "APPROVED")
	@NotNull(groups = {
		Default.class, OnCreate.class
	})
	@OneOf(value = {
		"PENDING", "APPROVED", "DEFICIENCY", "NOT_APPLICABLE"
	}, nullable = true, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String assessment;

	@Schema(description = "The text of the section", examples = "Inga betalningsanmärkningar finns registrerade.")
	private String text;

	@Schema(description = "User who completed the section", maxLength = 255, examples = "jo12doe")
	@Size(max = 255, groups = {
		Default.class, OnCreate.class, OnUpdate.class
	})
	private String completedBy;

	@Schema(description = "Timestamp when the section was completed", examples = "2021-09-28T12:00:00Z")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime completedAt;

	public static InvestigationSection create() {
		return new InvestigationSection();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public InvestigationSection withId(final String id) {
		this.id = id;
		return this;
	}

	public String getSectionKey() {
		return sectionKey;
	}

	public void setSectionKey(final String sectionKey) {
		this.sectionKey = sectionKey;
	}

	public InvestigationSection withSectionKey(final String sectionKey) {
		this.sectionKey = sectionKey;
		return this;
	}

	public String getHeading() {
		return heading;
	}

	public void setHeading(final String heading) {
		this.heading = heading;
	}

	public InvestigationSection withHeading(final String heading) {
		this.heading = heading;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public InvestigationSection withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public String getAssessment() {
		return assessment;
	}

	public void setAssessment(final String assessment) {
		this.assessment = assessment;
	}

	public InvestigationSection withAssessment(final String assessment) {
		this.assessment = assessment;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public InvestigationSection withText(final String text) {
		this.text = text;
		return this;
	}

	public String getCompletedBy() {
		return completedBy;
	}

	public void setCompletedBy(final String completedBy) {
		this.completedBy = completedBy;
	}

	public InvestigationSection withCompletedBy(final String completedBy) {
		this.completedBy = completedBy;
		return this;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public InvestigationSection withCompletedAt(final OffsetDateTime completedAt) {
		this.completedAt = completedAt;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sectionKey, heading, sortOrder, assessment, text, completedBy, completedAt);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final InvestigationSection other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(sectionKey, other.sectionKey)
			&& Objects.equals(heading, other.heading)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& Objects.equals(assessment, other.assessment)
			&& Objects.equals(text, other.text)
			&& Objects.equals(completedBy, other.completedBy)
			&& Objects.equals(completedAt, other.completedAt);
	}

	@Override
	public String toString() {
		return "InvestigationSection{" +
			"id='" + id + '\'' +
			", sectionKey='" + sectionKey + '\'' +
			", heading='" + heading + '\'' +
			", sortOrder=" + sortOrder +
			", assessment='" + assessment + '\'' +
			", text='" + text + '\'' +
			", completedBy='" + completedBy + '\'' +
			", completedAt=" + completedAt +
			'}';
	}
}
