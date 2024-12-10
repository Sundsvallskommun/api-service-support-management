package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.sql.Blob;
import java.util.Objects;

@Entity
@Table(name = "attachment_data")
public class AttachmentDataEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "file", columnDefinition = "longblob")
	@Lob
	private Blob file;

	public static AttachmentDataEntity create() {
		return new AttachmentDataEntity();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public AttachmentDataEntity withId(int id) {
		this.id = id;
		return this;
	}

	public Blob getFile() {
		return file;
	}

	public void setFile(Blob file) {
		this.file = file;
	}

	public AttachmentDataEntity withFile(Blob file) {
		this.file = file;
		return this;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("AttachmentDataEntity{");
		sb.append("id='").append(id).append('\'');
		sb.append(", file").append(file);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AttachmentDataEntity that = (AttachmentDataEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(file, that.file);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, file);
	}
}
