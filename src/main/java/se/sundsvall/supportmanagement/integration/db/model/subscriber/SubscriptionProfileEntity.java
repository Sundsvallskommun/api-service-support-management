package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A named set of event filters and the channels the events they match are delivered on. A subscription pointing at a
 * profile is governed by the profile, which is what lets a role be given a fixed notification setup. A profile without
 * channels leaves it to each subscriber how the events reach them.
 */
@Entity
@Table(name = "subscription_profile",
	indexes = {
		@Index(name = "idx_subscription_profile_municipality_id_namespace", columnList = "municipality_id, namespace")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_subscription_profile_municipality_namespace_name", columnNames = {
			"municipality_id", "namespace", "name"
		})
	})
public class SubscriptionProfileEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description")
	private String description;

	@ElementCollection
	@CollectionTable(name = "subscription_profile_event_filter",
		joinColumns = @JoinColumn(name = "profile_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_subscription_profile_event_filter_profile_id")))
	@OrderColumn(name = "sort_order")
	private List<EventFilterEmbeddable> eventFilters;

	@ElementCollection
	@CollectionTable(name = "subscription_profile_channel",
		joinColumns = @JoinColumn(name = "profile_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_subscription_profile_channel_profile_id")))
	@OrderColumn(name = "sort_order")
	@Column(name = "type", nullable = false, length = 32)
	private List<NotificationChannelType> channels;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static SubscriptionProfileEntity create() {
		return new SubscriptionProfileEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public SubscriptionProfileEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public SubscriptionProfileEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public SubscriptionProfileEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public SubscriptionProfileEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public SubscriptionProfileEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public List<EventFilterEmbeddable> getEventFilters() {
		return eventFilters;
	}

	public void setEventFilters(final List<EventFilterEmbeddable> eventFilters) {
		this.eventFilters = eventFilters;
	}

	public SubscriptionProfileEntity withEventFilters(final List<EventFilterEmbeddable> eventFilters) {
		this.eventFilters = eventFilters;
		return this;
	}

	public List<NotificationChannelType> getChannels() {
		return channels;
	}

	public void setChannels(final List<NotificationChannelType> channels) {
		this.channels = channels;
	}

	public SubscriptionProfileEntity withChannels(final List<NotificationChannelType> channels) {
		this.channels = channels;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriptionProfileEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public SubscriptionProfileEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, name, description, eventFilters, channels, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SubscriptionProfileEntity other = (SubscriptionProfileEntity) obj;
		return Objects.equals(id, other.id) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace)
			&& Objects.equals(name, other.name) && Objects.equals(description, other.description) && Objects.equals(eventFilters, other.eventFilters)
			&& Objects.equals(channels, other.channels) && Objects.equals(created, other.created) && Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "SubscriptionProfileEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", name='" + name + '\'' +
			", description='" + description + '\'' +
			", eventFilters=" + eventFilters +
			", channels=" + channels +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
