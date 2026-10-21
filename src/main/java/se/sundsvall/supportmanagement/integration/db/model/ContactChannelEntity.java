package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import static se.sundsvall.supportmanagement.integration.db.search.SearchAnalysisConfigurer.LOWERCASE;
import static se.sundsvall.supportmanagement.integration.db.search.SearchAnalysisConfigurer.TEXT;

@Embeddable
public class ContactChannelEntity {

	@Column(name = "type")
	@KeywordField(normalizer = LOWERCASE)
	private String type;

	// Both ways: an address or number is looked up exactly, but a search for a name also finds it inside an address
	@Column(name = "value")
	@KeywordField(name = "value_raw", normalizer = LOWERCASE)
	@FullTextField(analyzer = TEXT)
	private String value;

	public static ContactChannelEntity create() {
		return new ContactChannelEntity();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public ContactChannelEntity withType(final String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public ContactChannelEntity withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ContactChannelEntity that = (ContactChannelEntity) o;
		return Objects.equals(type, that.type) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public String toString() {
		return "ContactChannelEntity{" + "type='" + type + '\''
			+ ", value='" + value + '\''
			+ '}';
	}
}
