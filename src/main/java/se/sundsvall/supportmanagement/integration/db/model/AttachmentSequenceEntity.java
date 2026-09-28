package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * The last sequence number given to an attachment of an errand. A number once given is never given again within the
 * errand, not even when the attachment carrying it is removed. Removed together with the errand.
 */
@Entity
@Table(name = "attachment_sequence")
public class AttachmentSequenceEntity {

	@Id
	@Column(name = "errand_id")
	private String errandId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", foreignKey = @ForeignKey(name = "fk_attachment_sequence_errand_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ErrandEntity errandEntity;

	@Column(name = "last_sequence_number", nullable = false)
	private int lastSequenceNumber;

	public static AttachmentSequenceEntity create() {
		return new AttachmentSequenceEntity();
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public AttachmentSequenceEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public AttachmentSequenceEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public int getLastSequenceNumber() {
		return lastSequenceNumber;
	}

	public void setLastSequenceNumber(final int lastSequenceNumber) {
		this.lastSequenceNumber = lastSequenceNumber;
	}

	public AttachmentSequenceEntity withLastSequenceNumber(final int lastSequenceNumber) {
		this.lastSequenceNumber = lastSequenceNumber;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final AttachmentSequenceEntity that = (AttachmentSequenceEntity) o;
		return lastSequenceNumber == that.lastSequenceNumber && Objects.equals(errandId, that.errandId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, lastSequenceNumber);
	}

	@Override
	public String toString() {
		return "AttachmentSequenceEntity{" +
			"errandId='" + errandId + '\'' +
			", lastSequenceNumber=" + lastSequenceNumber +
			'}';
	}
}
