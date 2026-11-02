package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import static se.sundsvall.supportmanagement.integration.db.search.SearchAnalysisConfigurer.LOWERCASE;

@Entity
@Table(name = "contact_reason")
public class ContactReasonEntity extends AbstractMetadataEntity<ContactReasonEntity> {

	@Column(name = "reason")
	@KeywordField(normalizer = LOWERCASE)
	private String reason;

	public static ContactReasonEntity create() {
		return new ContactReasonEntity();
	}

	public String getReason() {
		return reason;
	}

	public void setReason(final String reason) {
		this.reason = reason;
	}

	public ContactReasonEntity withReason(final String reason) {
		this.reason = reason;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		return super.equals(o) && Objects.equals(reason, ((ContactReasonEntity) o).reason);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), reason);
	}

	@Override
	protected String fieldsToString() {
		return super.fieldsToString() + ", reason='" + reason + '\'';
	}
}
