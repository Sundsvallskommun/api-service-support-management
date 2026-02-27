package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "errand_action",
	indexes = {
		@Index(name = "idx_errand_action_errand_id", columnList = "errand_id"),
		@Index(name = "idx_errand_action_execute_after", columnList = "execute_after")
	})
public class ErrandActionEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_errand_action_errand_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ErrandEntity errandEntity;

	@Column(name = "execute_after")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime executeAfter;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "action_config_id", foreignKey = @ForeignKey(name = "fk_errand_action_action_config_id"))
	private ActionConfigEntity actionConfigEntity;

	public static ErrandActionEntity create() {
		return new ErrandActionEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandActionEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public ErrandActionEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public OffsetDateTime getExecuteAfter() {
		return executeAfter;
	}

	public void setExecuteAfter(final OffsetDateTime executeAfter) {
		this.executeAfter = executeAfter;
	}

	public ErrandActionEntity withExecuteAfter(final OffsetDateTime executeAfter) {
		this.executeAfter = executeAfter;
		return this;
	}

	public ActionConfigEntity getActionConfigEntity() {
		return actionConfigEntity;
	}

	public void setActionConfigEntity(final ActionConfigEntity actionConfigEntity) {
		this.actionConfigEntity = actionConfigEntity;
	}

	public ErrandActionEntity withActionConfigEntity(final ActionConfigEntity actionConfigEntity) {
		this.actionConfigEntity = actionConfigEntity;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ErrandActionEntity that = (ErrandActionEntity) o;
		return Objects.equals(id, that.id)
			&& Objects.equals(errandEntity != null ? errandEntity.getId() : null, that.errandEntity != null ? that.errandEntity.getId() : null)
			&& Objects.equals(executeAfter, that.executeAfter)
			&& Objects.equals(actionConfigEntity, that.actionConfigEntity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandEntity != null ? errandEntity.getId() : null, executeAfter, actionConfigEntity);
	}

	@Override
	public String toString() {
		return "ErrandActionEntity{" +
			"id='" + id + '\'' +
			", errandEntity=" + (errandEntity != null ? errandEntity.getId() : null) +
			", executeAfter=" + executeAfter +
			", actionConfigEntity=" + actionConfigEntity +
			'}';
	}
}
