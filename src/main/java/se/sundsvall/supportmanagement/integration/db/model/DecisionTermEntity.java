package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Length.LONG32;

/**
 * A term attached to a decision.
 * <p>
 * A permit term is a numbered line of text with a category, and it looks the same in a serving permit as in an
 * environmental permit. What is not shared - which beverages, which hours, which noise levels - stays in the text or in
 * the JSON parameters of the decision.
 */
@Entity
@Table(name = "decision_term",
	indexes = @Index(name = "idx_decision_term_decision_id", columnList = "decision_id"))
public class DecisionTermEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "decision_id", nullable = false, foreignKey = @ForeignKey(name = "fk_decision_term_decision_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DecisionEntity decisionEntity;

	@Column(name = "sort_order")
	private Integer sortOrder;

	/** The category of the term: "serveringstid", "brandskydd", "bullernivå". Metadata. */
	@Column(name = "category", length = 128)
	private String category;

	@Column(name = "text", length = LONG32, nullable = false)
	private String text;

	public static DecisionTermEntity create() {
		return new DecisionTermEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public DecisionTermEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public DecisionEntity getDecisionEntity() {
		return decisionEntity;
	}

	public void setDecisionEntity(final DecisionEntity decisionEntity) {
		this.decisionEntity = decisionEntity;
	}

	public DecisionTermEntity withDecisionEntity(final DecisionEntity decisionEntity) {
		this.decisionEntity = decisionEntity;
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public DecisionTermEntity withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public DecisionTermEntity withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public DecisionTermEntity withText(final String text) {
		this.text = text;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sortOrder, category, text);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DecisionTermEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(sortOrder, other.sortOrder)
			&& Objects.equals(category, other.category)
			&& Objects.equals(text, other.text);
	}

	@Override
	public String toString() {
		return "DecisionTermEntity{" +
			"id='" + id + '\'' +
			", decisionEntity=" + (decisionEntity != null ? decisionEntity.getId() : "null") +
			", sortOrder=" + sortOrder +
			", category='" + category + '\'' +
			", text='" + text + '\'' +
			'}';
	}
}
