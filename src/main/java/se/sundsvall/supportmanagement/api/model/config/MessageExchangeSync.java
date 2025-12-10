package se.sundsvall.supportmanagement.api.model.config;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "Message-Exchange sync configuration")
public class MessageExchangeSync {

	@Schema(description = "Unique id", accessMode = READ_ONLY, examples = "1")
	private Long id;

	@Schema(description = "Message exchange namespace to search in. Does not map to supporManagement namespace.", examples = "support")
	private String namespace;

	@Schema(description = "Latest synced sequence number", accessMode = READ_ONLY, examples = "333")
	private Long latestSyncedSequenceNumber;

	@Schema(description = "Timestamp when the configuration was last modified", examples = "2024-12-24T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	@Schema(description = "If set to true conversations will be synced")
	@NotNull
	private Boolean active;

	public static MessageExchangeSync create() {
		return new MessageExchangeSync();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MessageExchangeSync withId(Long id) {
		this.id = id;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public MessageExchangeSync withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public Long getLatestSyncedSequenceNumber() {
		return latestSyncedSequenceNumber;
	}

	public void setLatestSyncedSequenceNumber(Long latestSyncedSequenceNumber) {
		this.latestSyncedSequenceNumber = latestSyncedSequenceNumber;
	}

	public MessageExchangeSync withLatestSyncedSequenceNumber(Long latestSyncedSequenceNumber) {
		this.latestSyncedSequenceNumber = latestSyncedSequenceNumber;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public MessageExchangeSync withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public MessageExchangeSync withActive(Boolean active) {
		this.active = active;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		MessageExchangeSync that = (MessageExchangeSync) o;
		return Objects.equals(id, that.id) && Objects.equals(namespace, that.namespace) && Objects.equals(latestSyncedSequenceNumber, that.latestSyncedSequenceNumber) && Objects.equals(modified, that.modified)
			&& Objects.equals(active, that.active);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, namespace, latestSyncedSequenceNumber, modified, active);
	}

	@Override
	public String toString() {
		return "MessageExchangeSync{" +
			"id=" + id +
			", namespace='" + namespace + '\'' +
			", latestSyncedSequenceNumber=" + latestSyncedSequenceNumber +
			", modified=" + modified +
			", active=" + active +
			'}';
	}
}
