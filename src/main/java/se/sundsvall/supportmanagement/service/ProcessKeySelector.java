package se.sundsvall.supportmanagement.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.LabelAttributeEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;

/**
 * Which process an errand belongs to, according to its own labels.
 * <p>
 * Only the labels the errand actually wears are read - the tree is walked neither up nor down. The answer is read from
 * the attributes of the labels only, so renaming a label does not change it, and neither does moving it as long as the
 * errand wears the same labels.
 * <p>
 * The key is not checked against the processes that are deployed; the process engine answers 422 for a key it does
 * not recognise.
 */
@Component
public class ProcessKeySelector {

	/** The label attributes read here, matched exactly as spelled. */
	public static final String PROCESS_KEY_ATTRIBUTE = "processKey";
	public static final String PROCESS_START_MODE_ATTRIBUTE = "processStartMode";

	/** How much of a key is worth showing in a message that reports what is wrong with it. */
	private static final int KEY_EXCERPT_LENGTH = 64;

	private static final Logger LOG = LoggerFactory.getLogger(ProcessKeySelector.class);

	private final MetadataLabelRepository metadataLabelRepository;

	public ProcessKeySelector(final MetadataLabelRepository metadataLabelRepository) {
		this.metadataLabelRepository = metadataLabelRepository;
	}

	/**
	 * Reads the process key and the start mode of an errand out of its labels.
	 * <p>
	 * A label is read off the errand where Hibernate has filled it in, and looked up by id where it has not.
	 * {@link ErrandLabelEmbeddable#getMetadataLabel()} is filled in only when the errand is loaded, so the labels of an
	 * errand being created, and those a patch or a label action run as part of the write has just set, are looked up.
	 * The lookup is a single query, made only when such a label is there. A label the lookup does not find is passed
	 * over.
	 *
	 * @param  errand the errand to read.
	 * @return        the one key the labels agree on together with its start mode, or a selection naming every key found
	 *                when they agree on none or on more than one.
	 */
	public ProcessKeySelection select(final ErrandEntity errand) {
		return select(ofNullable(errand.getLabels()).orElse(emptyList()));
	}

	/**
	 * The same answer, read out of labels an errand wears or would wear.
	 * <p>
	 * A label is read where its metadata label is filled in, and looked up by id where it is not, in a single query made
	 * only when such a label is there. A label the lookup does not find is passed over.
	 *
	 * @param  labels the labels to read.
	 * @return        the one key the labels agree on together with its start mode, or a selection naming every key found
	 *                when they agree on none or on more than one.
	 */
	public ProcessKeySelection select(final Collection<ErrandLabelEmbeddable> labels) {
		final var loaded = labels.stream()
			.map(ErrandLabelEmbeddable::getMetadataLabel)
			.filter(Objects::nonNull)
			.toList();

		final var idsToLookUp = labels.stream()
			.filter(label -> isNull(label.getMetadataLabel()))
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.filter(Objects::nonNull)
			.collect(toSet());

		if (idsToLookUp.isEmpty()) {
			return selectFrom(loaded);
		}

		return selectFrom(Stream.concat(loaded.stream(), metadataLabelRepository.findAllById(idsToLookUp).stream()).toList());
	}

	/**
	 * The answer read out of the metadata labels themselves, deprecated ones passed over.
	 */
	private ProcessKeySelection selectFrom(final Collection<MetadataLabelEntity> labels) {
		final var candidates = labels.stream()
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
	 * How a key is named in a message about it.
	 * <p>
	 * Shortened, so that a message reporting what is wrong with a key is not made of the key. Used by the refusals and
	 * the error entries that name a key.
	 *
	 * @param  key the key to name.
	 * @return     the key, cut to the length worth showing.
	 */
	public static String excerptOf(final String key) {
		return StringUtils.abbreviate(key, KEY_EXCERPT_LENGTH);
	}

	/**
	 * The same, for a message that has to name every key an ambiguous errand resolves to.
	 *
	 * @param  keys the keys to name.
	 * @return      the keys, each cut to the length worth showing, separated by commas.
	 */
	public static String excerptOf(final Collection<String> keys) {
		return keys.stream()
			.map(ProcessKeySelector::excerptOf)
			.collect(joining(", "));
	}

	/**
	 * The mode of an errand whose labels all name the same process.
	 * <p>
	 * Two labels carrying the same key are one process, but they can still disagree about the mode. The mode is MANUAL
	 * when any of them says MANUAL, and AUTOMATIC otherwise.
	 */
	private ProcessStartMode startModeOf(final List<Candidate> candidates) {
		return candidates.stream().anyMatch(candidate -> MANUAL == candidate.startMode()) ? MANUAL : AUTOMATIC;
	}

	private Candidate toCandidate(final MetadataLabelEntity label) {
		final var key = attribute(label, PROCESS_KEY_ATTRIBUTE);

		return StringUtils.isBlank(key) ? null : new Candidate(key.trim(), toStartMode(label));
	}

	/**
	 * The start mode of one label, matched regardless of case: AUTOMATIC when the label says nothing about it, and MANUAL,
	 * with a warning logged, when its value names no start mode.
	 */
	private ProcessStartMode toStartMode(final MetadataLabelEntity label) {
		final var value = attribute(label, PROCESS_START_MODE_ATTRIBUTE);

		if (StringUtils.isBlank(value)) {
			return AUTOMATIC;
		}

		final var mode = EnumUtils.getEnumIgnoreCase(ProcessStartMode.class, value.trim());

		if (isNull(mode)) {
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

	/** One label's answer: its process key together with its own start mode. */
	private record Candidate(String key, ProcessStartMode startMode) {}
}
