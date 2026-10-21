package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Maps an errand to the Telia ACE {@code emailUid} of the work item created for it. One row per errand, keyed by
 * {@code errandId} itself; a row present means that errand already has a work item in Telia ACE, so the next
 * notification for it goes through {@code UpdateContactData} instead of {@code AddWorkItem}.
 */
@Entity
@Table(name = "telia_ace_contact")
public class TeliaAceContactEntity {

	@Id
	@Column(name = "errand_id", length = 36)
	private String errandId;

	@Column(name = "email_uid", nullable = false, length = 255)
	private String emailUid;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static TeliaAceContactEntity create() {
		return new TeliaAceContactEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public TeliaAceContactEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getEmailUid() {
		return emailUid;
	}

	public void setEmailUid(final String emailUid) {
		this.emailUid = emailUid;
	}

	public TeliaAceContactEntity withEmailUid(final String emailUid) {
		this.emailUid = emailUid;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public TeliaAceContactEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, emailUid, created);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final TeliaAceContactEntity other = (TeliaAceContactEntity) obj;
		return Objects.equals(errandId, other.errandId)
			&& Objects.equals(emailUid, other.emailUid)
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "TeliaAceContactEntity{" +
			"errandId='" + errandId + '\'' +
			", emailUid='" + emailUid + '\'' +
			", created=" + created +
			'}';
	}
}
