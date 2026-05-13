package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "subscription",
	indexes = {
		@Index(name = "idx_subscription_subscriber_id", columnList = "subscriber_id"),
		@Index(name = "idx_subscription_errand_id", columnList = "errand_id")
	})
public class SubscriptionEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subscriber_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_subscriber_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private SubscriberEntity subscriber;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 16)
	private SubscriptionTargetType targetType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", foreignKey = @ForeignKey(name = "fk_subscription_errand_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ErrandEntity errand;

	@ElementCollection
	@CollectionTable(name = "subscription_event_filter",
		joinColumns = @JoinColumn(name = "subscription_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_subscription_event_filter_subscription_id")))
	@OrderColumn(name = "sort_order")
	private List<EventFilterEmbeddable> eventFilters;

	@Column(name = "expires_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime expiresAt;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "type", column = @Column(name = "created_by_type", length = 16)),
		@AttributeOverride(name = "value", column = @Column(name = "created_by_value"))
	})
	private IdentifierEmbeddable createdBy;

	public static SubscriptionEntity create() {
		return new SubscriptionEntity();
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

	public SubscriptionEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public SubscriberEntity getSubscriber() {
		return subscriber;
	}

	public void setSubscriber(final SubscriberEntity subscriber) {
		this.subscriber = subscriber;
	}

	public SubscriptionEntity withSubscriber(final SubscriberEntity subscriber) {
		this.subscriber = subscriber;
		return this;
	}

	public SubscriptionTargetType getTargetType() {
		return targetType;
	}

	public void setTargetType(final SubscriptionTargetType targetType) {
		this.targetType = targetType;
	}

	public SubscriptionEntity withTargetType(final SubscriptionTargetType targetType) {
		this.targetType = targetType;
		return this;
	}

	public ErrandEntity getErrand() {
		return errand;
	}

	public void setErrand(final ErrandEntity errand) {
		this.errand = errand;
	}

	public SubscriptionEntity withErrand(final ErrandEntity errand) {
		this.errand = errand;
		return this;
	}

	public List<EventFilterEmbeddable> getEventFilters() {
		return eventFilters;
	}

	public void setEventFilters(final List<EventFilterEmbeddable> eventFilters) {
		this.eventFilters = eventFilters;
	}

	public SubscriptionEntity withEventFilters(final List<EventFilterEmbeddable> eventFilters) {
		this.eventFilters = eventFilters;
		return this;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(final OffsetDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public SubscriptionEntity withExpiresAt(final OffsetDateTime expiresAt) {
		this.expiresAt = expiresAt;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriptionEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public IdentifierEmbeddable getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final IdentifierEmbeddable createdBy) {
		this.createdBy = createdBy;
	}

	public SubscriptionEntity withCreatedBy(final IdentifierEmbeddable createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			id,
			subscriber != null ? subscriber.getId() : null,
			targetType,
			errand != null ? errand.getId() : null,
			eventFilters,
			expiresAt,
			created,
			createdBy);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SubscriptionEntity other = (SubscriptionEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(subscriber != null ? subscriber.getId() : null, other.subscriber != null ? other.subscriber.getId() : null)
			&& targetType == other.targetType
			&& Objects.equals(errand != null ? errand.getId() : null, other.errand != null ? other.errand.getId() : null)
			&& Objects.equals(eventFilters, other.eventFilters)
			&& Objects.equals(expiresAt, other.expiresAt)
			&& Objects.equals(created, other.created)
			&& Objects.equals(createdBy, other.createdBy);
	}

	@Override
	public String toString() {
		return "SubscriptionEntity{" +
			"id='" + id + '\'' +
			", subscriberId=" + (subscriber != null ? subscriber.getId() : null) +
			", targetType=" + targetType +
			", errandId=" + (errand != null ? errand.getId() : null) +
			", eventFilters=" + eventFilters +
			", expiresAt=" + expiresAt +
			", created=" + created +
			", createdBy=" + createdBy +
			'}';
	}
}
