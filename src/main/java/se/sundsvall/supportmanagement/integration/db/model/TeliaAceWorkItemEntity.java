package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Queued Telia ACE work item, waiting to be delivered by {@code TeliaAceWorkItemScheduler}. A row is only removed
 * once delivery has succeeded, so a row left behind after a failed attempt is retried on the next scheduler run.
 */
@Entity
@Table(name = "telia_ace_work_item",
	indexes = @Index(name = "idx_telia_ace_work_item_errand_id", columnList = "errand_id"))
public class TeliaAceWorkItemEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "from_address")
	private String fromAddress;

	@Column(name = "subject")
	private String subject;

	@Column(name = "content_url")
	private String contentUrl;

	@Column(name = "predefined_agent_name")
	private String predefinedAgentName;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static TeliaAceWorkItemEntity create() {
		return new TeliaAceWorkItemEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public TeliaAceWorkItemEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public TeliaAceWorkItemEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public TeliaAceWorkItemEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public TeliaAceWorkItemEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(final String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public TeliaAceWorkItemEntity withFromAddress(final String fromAddress) {
		this.fromAddress = fromAddress;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public TeliaAceWorkItemEntity withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getContentUrl() {
		return contentUrl;
	}

	public void setContentUrl(final String contentUrl) {
		this.contentUrl = contentUrl;
	}

	public TeliaAceWorkItemEntity withContentUrl(final String contentUrl) {
		this.contentUrl = contentUrl;
		return this;
	}

	public String getPredefinedAgentName() {
		return predefinedAgentName;
	}

	public void setPredefinedAgentName(final String predefinedAgentName) {
		this.predefinedAgentName = predefinedAgentName;
	}

	public TeliaAceWorkItemEntity withPredefinedAgentName(final String predefinedAgentName) {
		this.predefinedAgentName = predefinedAgentName;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public TeliaAceWorkItemEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, municipalityId, namespace, fromAddress, subject, contentUrl, predefinedAgentName, created);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final TeliaAceWorkItemEntity other = (TeliaAceWorkItemEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(fromAddress, other.fromAddress)
			&& Objects.equals(subject, other.subject)
			&& Objects.equals(contentUrl, other.contentUrl)
			&& Objects.equals(predefinedAgentName, other.predefinedAgentName)
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "TeliaAceWorkItemEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", fromAddress='" + fromAddress + '\'' +
			", subject='" + subject + '\'' +
			", contentUrl='" + contentUrl + '\'' +
			", predefinedAgentName='" + predefinedAgentName + '\'' +
			", created=" + created +
			'}';
	}
}
