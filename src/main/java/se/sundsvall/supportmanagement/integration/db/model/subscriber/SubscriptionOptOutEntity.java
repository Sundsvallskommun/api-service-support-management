package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Records that a subscriber has left a subscription profile, so the members sync does not subscribe them to it again
 * for as long as the record stands.
 */
@Entity
@Table(name = "subscription_opt_out",
	indexes = {
		@Index(name = "idx_subscription_opt_out_profile_id", columnList = "profile_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_subscription_opt_out_subscriber_profile", columnNames = {
			"subscriber_id", "profile_id"
		})
	})
public class SubscriptionOptOutEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subscriber_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_opt_out_subscriber_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private SubscriberEntity subscriber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_opt_out_profile_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private SubscriptionProfileEntity profile;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static SubscriptionOptOutEntity create() {
		return new SubscriptionOptOutEntity();
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

	public SubscriptionOptOutEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public SubscriberEntity getSubscriber() {
		return subscriber;
	}

	public void setSubscriber(final SubscriberEntity subscriber) {
		this.subscriber = subscriber;
	}

	public SubscriptionOptOutEntity withSubscriber(final SubscriberEntity subscriber) {
		this.subscriber = subscriber;
		return this;
	}

	public SubscriptionProfileEntity getProfile() {
		return profile;
	}

	public void setProfile(final SubscriptionProfileEntity profile) {
		this.profile = profile;
	}

	public SubscriptionOptOutEntity withProfile(final SubscriptionProfileEntity profile) {
		this.profile = profile;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriptionOptOutEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			id,
			subscriber != null ? subscriber.getId() : null,
			profile != null ? profile.getId() : null,
			created);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SubscriptionOptOutEntity other = (SubscriptionOptOutEntity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(subscriber != null ? subscriber.getId() : null, other.subscriber != null ? other.subscriber.getId() : null)
			&& Objects.equals(profile != null ? profile.getId() : null, other.profile != null ? other.profile.getId() : null)
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "SubscriptionOptOutEntity{" +
			"id='" + id + '\'' +
			", subscriberId=" + (subscriber != null ? subscriber.getId() : null) +
			", profileId=" + (profile != null ? profile.getId() : null) +
			", created=" + created +
			'}';
	}
}
