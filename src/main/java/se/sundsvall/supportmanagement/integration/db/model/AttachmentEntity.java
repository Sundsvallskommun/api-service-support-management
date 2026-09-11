package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.REMOVE;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "attachment",
	indexes = {
		@Index(name = "idx_attachment_file_name", columnList = "file_name"),
		@Index(name = "idx_attachment_municipality_id", columnList = "municipality_id"),
		@Index(name = "idx_attachment_namespace", columnList = "namespace"),
		@Index(name = "idx_attachment_attachment_purpose_id", columnList = "attachment_purpose_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_attachment_data_id", columnNames = {
			"attachment_data_id"
		})
	})
public class AttachmentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "namespace", length = 32)
	private String namespace;

	@Column(name = "municipality_id", length = 8)
	private String municipalityId;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "mime_type")
	private String mimeType;

	@Column(name = "channel")
	private String channel;

	@Column(name = "file_size")
	private Integer fileSize;

	@Column(name = "hash", length = 64)
	private String hash;

	/**
	 * What the attachment is for, as registered for the namespace. Optional - an attachment without one is shown as any
	 * other.
	 * <p>
	 * A property of the file rather than of any link to it, so the errand can show it in its own attachment list and an
	 * attachment belonging to no handling artefact can still carry one. The consequence is that it is a single value: an
	 * attachment serving one purpose for a statement serves the same purpose everywhere it is linked.
	 * <p>
	 * A reference rather than a copy of the name, the way the labels of an errand are, so a purpose given a new name or
	 * display name in the metadata is shown as such wherever it is used. Nothing cascades either way: clearing the
	 * reference leaves the purpose in the metadata, and a purpose still referenced cannot be removed from it.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "attachment_purpose_id", foreignKey = @ForeignKey(name = "fk_attachment_attachment_purpose_id"))
	private AttachmentPurposeEntity purpose;

	@ManyToOne(fetch = FetchType.LAZY, cascade = ALL)
	@JoinColumn(name = "attachment_data_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attachment_data_attachment"))
	private AttachmentDataEntity attachmentData;

	// The same column as the association above, mapped once more for reading only, so that the data row an attachment
	// points at can be named without the row being loaded. What that row holds is the file, and a removal that reaches
	// it through the association pays for every byte of it. Written through the association alone, which is what
	// insertable and updatable being false says.
	@Column(name = "attachment_data_id", nullable = false, insertable = false, updatable = false)
	private Integer attachmentDataId;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_errand_attachment_errand_id"))
	private ErrandEntity errandEntity;

	// The links to the handling artefacts this attachment is used by. cascade = REMOVE and nothing else: the attachment
	// tears down its links when it dies itself, but does not own them. A PERSIST cascade from here would resurrect
	// links the artefact side had just removed, which is the attachment resurrection of DRAKEN-4801 one level down.
	@OneToMany(mappedBy = "attachmentEntity", cascade = REMOVE)
	private List<StatementAttachmentEntity> statementLinks;

	@OneToMany(mappedBy = "attachmentEntity", cascade = REMOVE)
	private List<InvestigationAttachmentEntity> investigationLinks;

	@OneToMany(mappedBy = "attachmentEntity", cascade = REMOVE)
	private List<DecisionAttachmentEntity> decisionLinks;

	@OneToMany(mappedBy = "attachmentEntity", cascade = REMOVE)
	private List<MeasureAttachmentEntity> measureLinks;

	public static AttachmentEntity create() {
		return new AttachmentEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public AttachmentEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public AttachmentEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public AttachmentEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public AttachmentEntity withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public AttachmentEntity withMimeType(final String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(final String channel) {
		this.channel = channel;
	}

	public AttachmentEntity withChannel(final String channel) {
		this.channel = channel;
		return this;
	}

	public AttachmentDataEntity getAttachmentData() {
		return attachmentData;
	}

	public void setAttachmentData(final AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
	}

	public AttachmentEntity withAttachmentData(final AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
		return this;
	}

	/**
	 * The id of the data row this attachment points at, without the row being loaded. Read only: the association is
	 * what sets it, which is why there is no setter to go with this.
	 *
	 * @return the id of the data row, or null for an attachment that has not been written yet.
	 */
	public Integer getAttachmentDataId() {
		return attachmentDataId;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public AttachmentEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public List<StatementAttachmentEntity> getStatementLinks() {
		return statementLinks;
	}

	public void setStatementLinks(final List<StatementAttachmentEntity> statementLinks) {
		this.statementLinks = statementLinks;
	}

	public AttachmentEntity withStatementLinks(final List<StatementAttachmentEntity> statementLinks) {
		this.statementLinks = statementLinks;
		return this;
	}

	public List<InvestigationAttachmentEntity> getInvestigationLinks() {
		return investigationLinks;
	}

	public void setInvestigationLinks(final List<InvestigationAttachmentEntity> investigationLinks) {
		this.investigationLinks = investigationLinks;
	}

	public AttachmentEntity withInvestigationLinks(final List<InvestigationAttachmentEntity> investigationLinks) {
		this.investigationLinks = investigationLinks;
		return this;
	}

	public List<DecisionAttachmentEntity> getDecisionLinks() {
		return decisionLinks;
	}

	public void setDecisionLinks(final List<DecisionAttachmentEntity> decisionLinks) {
		this.decisionLinks = decisionLinks;
	}

	public AttachmentEntity withDecisionLinks(final List<DecisionAttachmentEntity> decisionLinks) {
		this.decisionLinks = decisionLinks;
		return this;
	}

	public List<MeasureAttachmentEntity> getMeasureLinks() {
		return measureLinks;
	}

	public void setMeasureLinks(final List<MeasureAttachmentEntity> measureLinks) {
		this.measureLinks = measureLinks;
	}

	public AttachmentEntity withMeasureLinks(final List<MeasureAttachmentEntity> measureLinks) {
		this.measureLinks = measureLinks;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public AttachmentEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public AttachmentEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Integer getFileSize() {
		return fileSize;
	}

	public void setFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
	}

	public AttachmentEntity withFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(final String hash) {
		this.hash = hash;
	}

	public AttachmentEntity withHash(final String hash) {
		this.hash = hash;
		return this;
	}

	public AttachmentPurposeEntity getPurpose() {
		return purpose;
	}

	public void setPurpose(final AttachmentPurposeEntity purpose) {
		this.purpose = purpose;
	}

	public AttachmentEntity withPurpose(final AttachmentPurposeEntity purpose) {
		this.purpose = purpose;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final AttachmentEntity that = (AttachmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(namespace, that.namespace) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(fileName, that.fileName) && Objects.equals(
			mimeType, that.mimeType) && Objects.equals(channel, that.channel) && Objects.equals(fileSize, that.fileSize) && Objects.equals(hash, that.hash) && Objects.equals(attachmentData, that.attachmentData)
			&& Objects.equals(
				created, that.created) && Objects.equals(modified, that.modified)
			&& Objects.equals(errandEntity, that.errandEntity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, namespace, municipalityId, fileName, mimeType, channel, fileSize, hash, attachmentData, created, modified, errandEntity);
	}

	@Override
	public String toString() {
		return "AttachmentEntity{" +
			"id='" + id + '\'' +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", fileName='" + fileName + '\'' +
			", mimeType='" + mimeType + '\'' +
			", channel='" + channel + '\'' +
			", fileSize=" + fileSize +
			", hash='" + hash + '\'' +
			", purpose=" + ofNullable(purpose).map(AttachmentPurposeEntity::getId).orElse(null) +
			", attachmentData=" + attachmentData +
			", created=" + created +
			", modified=" + modified +
			", errandEntity=" + errandEntity +
			'}';
	}
}
