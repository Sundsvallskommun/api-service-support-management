package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "communication_attachment_data")
public class CommunicationAttachmentDataEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(columnDefinition = "longblob")
	@Lob
	private Blob file;

	public static CommunicationAttachmentDataEntity create() {
		return new CommunicationAttachmentDataEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public CommunicationAttachmentDataEntity withId(final Long id) {
		this.id = id;
		return this;
	}

	public Blob getFile() {
		return file;
	}

	public void setFile(final Blob file) {
		this.file = file;
	}

	public CommunicationAttachmentDataEntity withFile(final Blob file) {
		this.file = file;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final CommunicationAttachmentDataEntity that = (CommunicationAttachmentDataEntity) o;
		return id == that.id && Objects.equals(file, that.file);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, file);
	}

	@Override
	public String toString() {
		return "CommunicationAttachmentDataEntity{" +
			"id=" + id +
			", file=" + file +
			'}';
	}

}
