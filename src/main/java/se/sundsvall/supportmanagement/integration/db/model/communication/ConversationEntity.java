package se.sundsvall.supportmanagement.integration.db.model.communication;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "conversation",
	indexes = {
		@Index(name = "idx_municipality_id_namespace_errand_id", columnList = "municipality_id, namespace, errand_id"),
		@Index(name = "idx_message_exchange_id", columnList = "message_exchange_id")
	})
public class ConversationEntity {

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "message_exchange_id", length = 36, nullable = false)
	private String messageExchangeId;

	@Column(name = "errand_id", length = 36, nullable = false)
	private String errandId;

	@Column(name = "namespace", length = 32, nullable = false)
	private String namespace;

	@Column(name = "municipality_id", length = 4, nullable = false)
	private String municipalityId;

	@Column(name = "topic")
	private String topic;

	@Column(name = "type", length = 32, nullable = false)
	private String type;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "conversation_relation_id", joinColumns = @JoinColumn(name = "conversation_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_conversation_relation_conversation_id")))
	@Column(name = "relation_id", length = 36)
	private List<String> relationIds;

	@Column(name = "latest_synced_sequence_number")
	private Long latestSyncedSequenceNumber;

	public static ConversationEntity create() {
		return new ConversationEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ConversationEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getMessageExchangeId() {
		return messageExchangeId;
	}

	public void setMessageExchangeId(String messageExchangeId) {
		this.messageExchangeId = messageExchangeId;
	}

	public ConversationEntity withMessageExchangeId(String messageExchangeId) {
		this.messageExchangeId = messageExchangeId;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(String errandId) {
		this.errandId = errandId;
	}

	public ConversationEntity withErrandId(String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public ConversationEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ConversationEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public ConversationEntity withTopic(String topic) {
		this.topic = topic;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ConversationEntity withType(String type) {
		this.type = type;
		return this;
	}

	public List<String> getRelationIds() {
		return relationIds;
	}

	public void setRelationIds(List<String> relationIds) {
		this.relationIds = relationIds;
	}

	public ConversationEntity withRelationIds(List<String> relationIds) {
		this.relationIds = relationIds;
		return this;
	}

	public Long getLatestSyncedSequenceNumber() {
		return latestSyncedSequenceNumber;
	}

	public void setLatestSyncedSequenceNumber(Long latestSyncedSequenceNumber) {
		this.latestSyncedSequenceNumber = latestSyncedSequenceNumber;
	}

	public ConversationEntity withLatestSyncedSequenceNumber(Long latestSyncedSequenceNumber) {
		this.latestSyncedSequenceNumber = latestSyncedSequenceNumber;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		ConversationEntity that = (ConversationEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(messageExchangeId, that.messageExchangeId) && Objects.equals(errandId, that.errandId) && Objects.equals(namespace, that.namespace)
			&& Objects.equals(municipalityId, that.municipalityId) && Objects.equals(topic, that.topic) && Objects.equals(type, that.type) && Objects.equals(relationIds, that.relationIds) && Objects.equals(
				latestSyncedSequenceNumber, that.latestSyncedSequenceNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, messageExchangeId, errandId, namespace, municipalityId, topic, type, relationIds, latestSyncedSequenceNumber);
	}

	@Override
	public String toString() {
		return "ConversationEntity{" +
			"id='" + id + '\'' +
			", messageExchangeId='" + messageExchangeId + '\'' +
			", errandId='" + errandId + '\'' +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", topic='" + topic + '\'' +
			", type='" + type + '\'' +
			", relationIds=" + relationIds +
			", latestSyncedSequenceNumber=" + latestSyncedSequenceNumber +
			'}';
	}
}
