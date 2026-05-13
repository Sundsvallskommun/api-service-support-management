package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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

import static jakarta.persistence.CascadeType.ALL;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "subscriber",
	indexes = {
		@Index(name = "idx_subscriber_municipality_id_namespace", columnList = "municipality_id, namespace"),
		@Index(name = "idx_subscriber_municipality_id_namespace_identifier", columnList = "municipality_id, namespace, identifier_type, identifier_value")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_subscriber_municipality_namespace_identifier_name", columnNames = {
			"municipality_id", "namespace", "identifier_type", "identifier_value", "name"
		})
	})
public class SubscriberEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "name")
	private String name;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "type", column = @Column(name = "identifier_type", nullable = false, length = 16)),
		@AttributeOverride(name = "value", column = @Column(name = "identifier_value", nullable = false))
	})
	private IdentifierEmbeddable identifier;

	@ElementCollection
	@CollectionTable(name = "subscriber_channel",
		joinColumns = @JoinColumn(name = "subscriber_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_subscriber_channel_subscriber_id")))
	@OrderColumn(name = "sort_order")
	private List<NotificationChannelEmbeddable> channels;

	@ElementCollection
	@CollectionTable(name = "subscriber_event_filter",
		joinColumns = @JoinColumn(name = "subscriber_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_subscriber_event_filter_subscriber_id")))
	@OrderColumn(name = "sort_order")
	private List<EventFilterEmbeddable> eventFilters;

	@Column(name = "paused_from")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime pausedFrom;

	@Column(name = "paused_until")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime pausedUntil;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "type", column = @Column(name = "created_by_type", length = 16)),
		@AttributeOverride(name = "value", column = @Column(name = "created_by_value"))
	})
	private IdentifierEmbeddable createdBy;

	@OneToMany(mappedBy = "subscriber", cascade = ALL, orphanRemoval = true)
	private List<SubscriptionEntity> subscriptions;

	public static SubscriberEntity create() {
		return new SubscriberEntity();
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

	public SubscriberEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public SubscriberEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public SubscriberEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public SubscriberEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public IdentifierEmbeddable getIdentifier() {
		return identifier;
	}

	public void setIdentifier(final IdentifierEmbeddable identifier) {
		this.identifier = identifier;
	}

	public SubscriberEntity withIdentifier(final IdentifierEmbeddable identifier) {
		this.identifier = identifier;
		return this;
	}

	public List<NotificationChannelEmbeddable> getChannels() {
		return channels;
	}

	public void setChannels(final List<NotificationChannelEmbeddable> channels) {
		this.channels = channels;
	}

	public SubscriberEntity withChannels(final List<NotificationChannelEmbeddable> channels) {
		this.channels = channels;
		return this;
	}

	public List<EventFilterEmbeddable> getEventFilters() {
		return eventFilters;
	}

	public void setEventFilters(final List<EventFilterEmbeddable> eventFilters) {
		this.eventFilters = eventFilters;
	}

	public SubscriberEntity withEventFilters(final List<EventFilterEmbeddable> eventFilters) {
		this.eventFilters = eventFilters;
		return this;
	}

	public OffsetDateTime getPausedFrom() {
		return pausedFrom;
	}

	public void setPausedFrom(final OffsetDateTime pausedFrom) {
		this.pausedFrom = pausedFrom;
	}

	public SubscriberEntity withPausedFrom(final OffsetDateTime pausedFrom) {
		this.pausedFrom = pausedFrom;
		return this;
	}

	public OffsetDateTime getPausedUntil() {
		return pausedUntil;
	}

	public void setPausedUntil(final OffsetDateTime pausedUntil) {
		this.pausedUntil = pausedUntil;
	}

	public SubscriberEntity withPausedUntil(final OffsetDateTime pausedUntil) {
		this.pausedUntil = pausedUntil;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriberEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public SubscriberEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public IdentifierEmbeddable getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final IdentifierEmbeddable createdBy) {
		this.createdBy = createdBy;
	}

	public SubscriberEntity withCreatedBy(final IdentifierEmbeddable createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public List<SubscriptionEntity> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(final List<SubscriptionEntity> subscriptions) {
		this.subscriptions = subscriptions;
	}

	public SubscriberEntity withSubscriptions(final List<SubscriptionEntity> subscriptions) {
		this.subscriptions = subscriptions;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, name, identifier, channels, eventFilters, pausedFrom, pausedUntil, created, modified, createdBy);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SubscriberEntity other = (SubscriberEntity) obj;
		return Objects.equals(id, other.id) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace)
			&& Objects.equals(name, other.name) && Objects.equals(identifier, other.identifier) && Objects.equals(channels, other.channels)
			&& Objects.equals(eventFilters, other.eventFilters) && Objects.equals(pausedFrom, other.pausedFrom) && Objects.equals(pausedUntil, other.pausedUntil)
			&& Objects.equals(created, other.created) && Objects.equals(modified, other.modified) && Objects.equals(createdBy, other.createdBy);
	}

	@Override
	public String toString() {
		return "SubscriberEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", name='" + name + '\'' +
			", identifier=" + identifier +
			", channels=" + channels +
			", eventFilters=" + eventFilters +
			", pausedFrom=" + pausedFrom +
			", pausedUntil=" + pausedUntil +
			", created=" + created +
			", modified=" + modified +
			", createdBy=" + createdBy +
			'}';
	}
}
