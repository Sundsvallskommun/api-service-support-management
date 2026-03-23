package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Errand phase model")
public class ErrandPhase {

	@Schema(description = "Unique errand phase record ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Phase name", examples = "INVESTIGATION", accessMode = READ_ONLY)
	private String name;

	@Schema(description = "Phase display name", examples = "Utredning", accessMode = READ_ONLY)
	private String displayName;

	@Schema(description = "Timestamp when the errand entered this phase", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime started;

	@Schema(description = "Timestamp when the errand left this phase", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime ended;

	public static ErrandPhase create() {
		return new ErrandPhase();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandPhase withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ErrandPhase withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public ErrandPhase withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public OffsetDateTime getStarted() {
		return started;
	}

	public void setStarted(final OffsetDateTime started) {
		this.started = started;
	}

	public ErrandPhase withStarted(final OffsetDateTime started) {
		this.started = started;
		return this;
	}

	public OffsetDateTime getEnded() {
		return ended;
	}

	public void setEnded(final OffsetDateTime ended) {
		this.ended = ended;
	}

	public ErrandPhase withEnded(final OffsetDateTime ended) {
		this.ended = ended;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(displayName, ended, id, name, started);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ErrandPhase other)) {
			return false;
		}
		return Objects.equals(displayName, other.displayName) && Objects.equals(ended, other.ended) && Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(started, other.started);
	}

	@Override
	public String toString() {
		return "ErrandPhase{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", started=" + started +
			", ended=" + ended +
			'}';
	}
}
