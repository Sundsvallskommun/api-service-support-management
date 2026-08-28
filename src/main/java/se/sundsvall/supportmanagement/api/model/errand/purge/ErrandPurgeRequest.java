package se.sundsvall.supportmanagement.api.model.errand.purge;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.validation.ValidPurgeCutoff;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Instruction for a purge run.
 * <p>
 * The cutoff is an explicit point in time rather than a retention period, which keeps a run reproducible regardless of
 * when it is started and lets the same resource serve any retention rule the caller cares to apply.
 * <p>
 * Purging cannot be undone, so dryRun carries no default. Leaving it out is far more likely to be an oversight than a
 * considered request to erase several hundred thousand errands, and a caller that has to write the intent out cannot
 * make that mistake silently.
 */
@Schema(description = "Errand purge request model")
public class ErrandPurgeRequest {

	@Schema(description = "Errands last touched before this point in time are purged", examples = "2024-08-28T00:00:00.000+02:00", requiredMode = REQUIRED)
	@NotNull
	@ValidPurgeCutoff
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime olderThan;

	@Schema(description = "When true, the run only counts the errands that would be purged and deletes nothing", examples = "true", requiredMode = REQUIRED)
	@NotNull
	private Boolean dryRun;

	@Schema(description = "Highest number of errands to handle in this run. Unlimited when omitted.", examples = "1000")
	@Min(1)
	private Integer maxErrands;

	public static ErrandPurgeRequest create() {
		return new ErrandPurgeRequest();
	}

	public OffsetDateTime getOlderThan() {
		return olderThan;
	}

	public void setOlderThan(final OffsetDateTime olderThan) {
		this.olderThan = olderThan;
	}

	public ErrandPurgeRequest withOlderThan(final OffsetDateTime olderThan) {
		this.olderThan = olderThan;
		return this;
	}

	public Boolean getDryRun() {
		return dryRun;
	}

	public void setDryRun(final Boolean dryRun) {
		this.dryRun = dryRun;
	}

	public ErrandPurgeRequest withDryRun(final Boolean dryRun) {
		this.dryRun = dryRun;
		return this;
	}

	public Integer getMaxErrands() {
		return maxErrands;
	}

	public void setMaxErrands(final Integer maxErrands) {
		this.maxErrands = maxErrands;
	}

	public ErrandPurgeRequest withMaxErrands(final Integer maxErrands) {
		this.maxErrands = maxErrands;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(olderThan, dryRun, maxErrands);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ErrandPurgeRequest that = (ErrandPurgeRequest) o;
		return Objects.equals(olderThan, that.olderThan)
			&& Objects.equals(dryRun, that.dryRun)
			&& Objects.equals(maxErrands, that.maxErrands);
	}

	@Override
	public String toString() {
		return "ErrandPurgeRequest{" +
			"olderThan=" + olderThan +
			", dryRun=" + dryRun +
			", maxErrands=" + maxErrands +
			'}';
	}
}
