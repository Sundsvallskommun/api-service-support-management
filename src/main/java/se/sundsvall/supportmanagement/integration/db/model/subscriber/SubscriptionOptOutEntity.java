package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
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
 * Records that a principal has left a subscription profile, so the members sync does not subscribe them to it again
 * for as long as the record stands.
 * <p>
 * Kept against the principal rather than one of their subscribers, so that it holds whichever subscriber the sync
 * would use, and outlives the principal removing their subscribers altogether.
 */
@Entity
@Table(name = "subscription_opt_out",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_subscription_opt_out_profile_identifier", columnNames = {
			"profile_id", "identifier_type", "identifier_value"
		})
	})
public class SubscriptionOptOutEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_opt_out_profile_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private SubscriptionProfileEntity profile;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "type", column = @Column(name = "identifier_type", nullable = false, length = 16)),
		@AttributeOverride(name = "value", column = @Column(name = "identifier_value", nullable = false))
	})
	private IdentifierEmbeddable identifier;

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

	public IdentifierEmbeddable getIdentifier() {
		return identifier;
	}

	public void setIdentifier(final IdentifierEmbeddable identifier) {
		this.identifier = identifier;
	}

	public SubscriptionOptOutEntity withIdentifier(final IdentifierEmbeddable identifier) {
		this.identifier = identifier;
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
		return Objects.hash(id, profile != null ? profile.getId() : null, identifier, created);
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
			&& Objects.equals(profile != null ? profile.getId() : null, other.profile != null ? other.profile.getId() : null)
			&& Objects.equals(identifier, other.identifier)
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "SubscriptionOptOutEntity{" +
			"id='" + id + '\'' +
			", profileId=" + (profile != null ? profile.getId() : null) +
			", identifier=" + identifier +
			", created=" + created +
			'}';
	}
}
