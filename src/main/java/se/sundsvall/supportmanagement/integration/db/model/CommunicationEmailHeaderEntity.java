package se.sundsvall.supportmanagement.integration.db.model;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

@Entity
@Table(name = "communication_email_header")
public class CommunicationEmailHeaderEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Enumerated(EnumType.STRING)
	@Column(name = "header_key")
	private EmailHeader header;

	@ElementCollection
	@CollectionTable(name = "communication_email_header_value",
		joinColumns = @JoinColumn(name = "header_id",
			referencedColumnName = "id",
			foreignKey = @ForeignKey(name = "fk_header_value_header_id")))
	@Column(name = "value", length = 2048)
	@OrderColumn(name = "order_index")
	private List<String> values;

	public static CommunicationEmailHeaderEntity create() {
		return new CommunicationEmailHeaderEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public CommunicationEmailHeaderEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public EmailHeader getHeader() {
		return header;
	}

	public void setHeader(final EmailHeader header) {
		this.header = header;
	}

	public CommunicationEmailHeaderEntity withHeader(final EmailHeader header) {
		this.header = header;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public CommunicationEmailHeaderEntity withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final CommunicationEmailHeaderEntity that = (CommunicationEmailHeaderEntity) o;
		return Objects.equals(id, that.id) && header == that.header && Objects.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, header, values);
	}

	@Override
	public String toString() {
		return "CommunicationEmailHeaderEntity{" +
			"id='" + id + '\'' +
			", header=" + header +
			", values=" + values +
			'}';
	}

}
