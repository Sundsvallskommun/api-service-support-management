package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Fields that are copied automatically to the target namespace without manual mapping")
public class DirectlyCopyable {

	@Schema(description = "Title of the source errand", examples = "Trasig dörr på Storgatan")
	private String title;

	@Schema(description = "Priority of the source errand", examples = "HIGH")
	private String priority;

	@Schema(description = "Number of stakeholders that will be copied", examples = "3")
	private Integer stakeholderCount;

	@Schema(description = "Number of external tags that will be copied", examples = "5")
	private Integer externalTagCount;

	@Schema(description = "Number of attachments that will be copied", examples = "2")
	private Integer attachmentCount;

	public static DirectlyCopyable create() {
		return new DirectlyCopyable();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public DirectlyCopyable withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public DirectlyCopyable withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public Integer getStakeholderCount() {
		return stakeholderCount;
	}

	public void setStakeholderCount(final Integer stakeholderCount) {
		this.stakeholderCount = stakeholderCount;
	}

	public DirectlyCopyable withStakeholderCount(final Integer stakeholderCount) {
		this.stakeholderCount = stakeholderCount;
		return this;
	}

	public Integer getExternalTagCount() {
		return externalTagCount;
	}

	public void setExternalTagCount(final Integer externalTagCount) {
		this.externalTagCount = externalTagCount;
	}

	public DirectlyCopyable withExternalTagCount(final Integer externalTagCount) {
		this.externalTagCount = externalTagCount;
		return this;
	}

	public Integer getAttachmentCount() {
		return attachmentCount;
	}

	public void setAttachmentCount(final Integer attachmentCount) {
		this.attachmentCount = attachmentCount;
	}

	public DirectlyCopyable withAttachmentCount(final Integer attachmentCount) {
		this.attachmentCount = attachmentCount;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, priority, stakeholderCount, externalTagCount, attachmentCount);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DirectlyCopyable other)) {
			return false;
		}
		return Objects.equals(title, other.title) && Objects.equals(priority, other.priority)
			&& Objects.equals(stakeholderCount, other.stakeholderCount) && Objects.equals(externalTagCount, other.externalTagCount)
			&& Objects.equals(attachmentCount, other.attachmentCount);
	}

	@Override
	public String toString() {
		return "DirectlyCopyable [title=" + title + ", priority=" + priority + ", stakeholderCount=" + stakeholderCount
			+ ", externalTagCount=" + externalTagCount + ", attachmentCount=" + attachmentCount + "]";
	}
}
