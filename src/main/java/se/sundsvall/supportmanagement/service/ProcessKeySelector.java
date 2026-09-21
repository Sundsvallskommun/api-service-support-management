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
	 * errand being created, and those a patch or a label action run as part of the write has just set, all point at
	 * nothing. Read as they stand, precisely the writes that give an errand its process label would name no process, and
	 * start none. The lookup is a single query, made only when such a label is there, so an errand read from the database
	 * costs nothing more than before. A label the lookup does not find is passed over rather than thrown on, as one that
	 * is gone always has been.
	 *
	 * @param  errand the errand to read.
	 * @return        the one key the labels agree on together with its start mode, or a selection naming every key found
	 *                when they agree on none or on more than one.
	 */
	public ProcessKeySelection select(final ErrandEntity errand) {
		final var labels = ofNullable(errand.getLabels()).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.toList();

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
	 * The same answer, read out of labels rather than out of an errand.
	 * <p>
	 * For the question asked about labels an errand does not wear yet: whether changing them would move its process key.
	 * {@link ErrandLabelEmbeddable#getMetadataLabel()} is filled in by Hibernate when the errand is loaded and is null on
	 * a label that has only just been put together, so the caller asking that question looks the labels up itself and
	 * hands them here.
	 *
	 * @param  labels the labels to read.
	 * @return        the one key they agree on together with its start mode, or a selection naming every key found when
	 *                they agree on none or on more than one.
	 */
	public ProcessKeySelection selectFrom(final Collection<MetadataLabelEntity> labels) {
		final var candidates = ofNullable(labels).orElse(emptyList()).stream()
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
	 * How a key is named in a message about it.
	 * <p>
	 * Shortened, since a message reporting what is wrong with a key may not be made of the key. It is held here, on the
	 * class that owns what a process key is, rather than beside each message: the refusals and the error entries that
	 * name a key are written in more than one place, and the one that has no outer limit of its own - the detail of a
	 * 400 - is the one that needs this most.
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
		return ofNullable(keys).orElse(emptyList()).stream()
			.map(ProcessKeySelector::excerptOf)
			.collect(joining(", "));
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
			// The label write refuses such a value, so this one came in past the API - straight into the database, or before
			// the check existed. Someone put it there on purpose, so the answer is the one that cannot start a process nobody
			// asked for - and the errand is still startable by hand.
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
