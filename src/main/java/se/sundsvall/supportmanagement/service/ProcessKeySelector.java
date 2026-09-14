package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.LabelAttributeEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;

/**
 * Which process an errand belongs to, according to its own labels.
 * <p>
 * Only the labels the errand actually wears are read - the tree is walked neither up nor down. Were it walked, a
 * process could start to apply to an errand because someone moved a label in the metadata, and nothing about the errand
 * itself would have changed. For the same reason the resolution hangs on the attribute rather than on what the label is
 * called or where it sits, so renaming a label or moving it leaves the answer alone.
 * <p>
 * SM does not check that the key names a process that exists. Only the process engine knows what is deployed, and it
 * answers 422 for a key it does not recognise.
 */
@Component
public class ProcessKeySelector {

	static final String PROCESS_KEY_ATTRIBUTE = "processKey";
	static final String PROCESS_START_MODE_ATTRIBUTE = "processStartMode";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessKeySelector.class);

	/**
	 * Reads the process key and the start mode of an errand out of its labels.
	 *
	 * @param  errand the errand to read.
	 * @return        the one key the labels agree on together with its start mode, or a selection naming every key found
	 *                when they agree on none or on more than one.
	 */
	public ProcessKeySelection select(final ErrandEntity errand) {
		final var candidates = ofNullable(errand.getLabels()).orElse(emptyList()).stream()
			.map(ErrandLabelEmbeddable::getMetadataLabel)
			.filter(Objects::nonNull)
			.filter(label -> !label.isDeprecated())
			.map(this::toCandidate)
			.filter(Objects::nonNull)
			.toList();

		final var keys = candidates.stream()
			.map(Candidate::key)
			.distinct()
			.sorted()
			.toList();

		if (keys.size() != 1) {
			return keys.isEmpty() ? ProcessKeySelection.NONE : new ProcessKeySelection(null, null, keys);
		}

		return new ProcessKeySelection(keys.getFirst(), startModeOf(candidates), keys);
	}

	/**
	 * The mode of an errand whose labels all name the same process.
	 * <p>
	 * Two labels carrying the same key are one process, but they can still disagree about the mode. A MANUAL among them
	 * is a person saying that nothing should start on its own, and that answer wins over the other label's silence.
	 */
	private ProcessStartMode startModeOf(final List<Candidate> candidates) {
		return candidates.stream().anyMatch(candidate -> MANUAL == candidate.startMode()) ? MANUAL : AUTOMATIC;
	}

	private Candidate toCandidate(final MetadataLabelEntity label) {
		final var key = attribute(label, PROCESS_KEY_ATTRIBUTE);

		return StringUtils.isBlank(key) ? null : new Candidate(key.trim(), toStartMode(label));
	}

	/**
	 * The start mode of one label. A label that says nothing about it behaves as it did before the attribute existed.
	 */
	private ProcessStartMode toStartMode(final MetadataLabelEntity label) {
		final var value = attribute(label, PROCESS_START_MODE_ATTRIBUTE);

		if (StringUtils.isBlank(value)) {
			return AUTOMATIC;
		}

		final var mode = EnumUtils.getEnumIgnoreCase(ProcessStartMode.class, value.trim());

		if (isNull(mode)) {
			// Until the label write refuses a value this cannot read, one can be sitting there. Someone put it there on
			// purpose, so the answer is the one that cannot start a process nobody asked for - and the errand is still
			// startable by hand.
			LOG.warn("Label '{}' carries an unreadable process start mode '{}' and is treated as MANUAL", sanitizeForLogging(label.getId()), sanitizeForLogging(value));
			return MANUAL;
		}

		return mode;
	}

	private String attribute(final MetadataLabelEntity label, final String key) {
		return ofNullable(label.getAttributes()).orElse(emptyList()).stream()
			.filter(attribute -> key.equals(attribute.getKey()))
			.map(LabelAttributeEmbeddable::getValue)
			.findFirst()
			.orElse(null);
	}

	/** One label's answer, held together so that the mode can never be taken from another label than the key was. */
	private record Candidate(String key, ProcessStartMode startMode) {}
}
