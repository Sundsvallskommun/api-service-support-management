package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static jakarta.persistence.FetchType.LAZY;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "email_dispatch_outbox",
	indexes = {
		@Index(name = "idx_email_dispatch_outbox_subscriber_created", columnList = "subscriber_id, created")
	})
public class EmailDispatchOutboxEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	// The database removes the rows of a subscriber that is deleted, so nothing is left to be sent to them
	@ManyToOne(fetch = LAZY, optional = false)
	@JoinColumn(name = "subscriber_id", nullable = false, foreignKey = @ForeignKey(name = "fk_email_dispatch_outbox_subscriber_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private SubscriberEntity subscriber;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "errand_number")
	private String errandNumber;

	@ElementCollection
	@CollectionTable(name = "email_dispatch_outbox_event",
		joinColumns = @JoinColumn(name = "outbox_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_email_dispatch_outbox_event_outbox_id")))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<EmailDispatchOutboxEventEmbeddable> events;

	@Column(name = "created", nullable = false, columnDefinition = "datetime(3)")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static EmailDispatchOutboxEntity create() {
		return new EmailDispatchOutboxEntity();
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

	public EmailDispatchOutboxEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public SubscriberEntity getSubscriber() {
		return subscriber;
	}

	public void setSubscriber(final SubscriberEntity subscriber) {
		this.subscriber = subscriber;
	}

	public EmailDispatchOutboxEntity withSubscriber(final SubscriberEntity subscriber) {
		this.subscriber = subscriber;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public EmailDispatchOutboxEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public EmailDispatchOutboxEntity withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public List<EmailDispatchOutboxEventEmbeddable> getEvents() {
		return events;
	}

	public void setEvents(final List<EmailDispatchOutboxEventEmbeddable> events) {
		this.events = events;
	}

	public EmailDispatchOutboxEntity withEvents(final List<EmailDispatchOutboxEventEmbeddable> events) {
		this.events = events;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public EmailDispatchOutboxEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, subscriberId(), errandId, errandNumber, events, created);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final EmailDispatchOutboxEntity other = (EmailDispatchOutboxEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(subscriberId(), other.subscriberId())
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(errandNumber, other.errandNumber)
			&& Objects.equals(events, other.events)
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "EmailDispatchOutboxEntity{" +
			"id='" + id + '\'' +
			", subscriberId='" + subscriberId() + '\'' +
			", errandId='" + errandId + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", events=" + events +
			", created=" + created +
			'}';
	}

	private String subscriberId() {
		return Optional.ofNullable(subscriber).map(SubscriberEntity::getId).orElse(null);
	}
}
