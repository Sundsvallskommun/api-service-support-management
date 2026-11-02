package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.Strings;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;
import se.sundsvall.supportmanagement.api.validation.ValidProcessLabelAttributes;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode;
import se.sundsvall.supportmanagement.service.ProcessRules;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper.escapeMessageParameter;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_BLOCKED_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_KEY_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.PROCESS_START_MODE_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.ProcessKeySelector.excerptOf;

/**
 * Validates the process attributes of a label against five rules, and reports each fault as a violation of its own:
 *
 * <pre>
 * a key equal to processKey, processStartMode or processBlocked when case and surrounding blanks are ignored, but not
 *   spelled exactly so
 * a processStartMode other than exactly AUTOMATIC or MANUAL
 * a processStartMode without a processKey
 * a processKey longer than a process key may be
 * a processBlocked other than exactly true or false
 * </pre>
 */
public class ValidProcessLabelAttributesConstraintValidator implements ConstraintValidator<ValidProcessLabelAttributes, Collection<LabelAttribute>> {

	private static final List<String> READ_ATTRIBUTES = List.of(PROCESS_KEY_ATTRIBUTE, PROCESS_START_MODE_ATTRIBUTE, PROCESS_BLOCKED_ATTRIBUTE);
	private static final List<String> START_MODES = Arrays.stream(ProcessStartMode.values()).map(Enum::name).toList();
	private static final List<String> BLOCKED_VALUES = List.of(TRUE.toString(), FALSE.toString());

	private static final String MISSPELLED_KEY = "the attribute '%s' is read only when spelled exactly '%s'";
	private static final String UNKNOWN_START_MODE = "the processStartMode '%s' must be exactly one of %s";
	private static final String START_MODE_WITHOUT_KEY = "a processStartMode needs a processKey on the same label, since a start mode means nothing without the process it starts";
	private static final String OVERSIZED_KEY = "the processKey has %d characters, and a process key may hold at most %d";
	private static final String UNKNOWN_BLOCKED = "the processBlocked '%s' must be exactly one of %s";

	@Override
	public boolean isValid(final Collection<LabelAttribute> value, final ConstraintValidatorContext context) {
		final var faults = new ArrayList<String>();
		collectFaults(ofNullable(value).orElse(emptyList()), faults);

		if (faults.isEmpty()) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		faults.forEach(fault -> context.buildConstraintViolationWithTemplate(escapeMessageParameter(fault)).addConstraintViolation());

		return false;
	}

	private static void collectFaults(final Collection<LabelAttribute> attributes, final List<String> faults) {
		final var present = attributes.stream()
			.filter(Objects::nonNull)
			.filter(attribute -> nonNull(attribute.getKey()))
			.toList();

		present.forEach(attribute -> READ_ATTRIBUTES.stream()
			.filter(read -> isMisspelled(attribute.getKey(), read))
			.findFirst()
			.ifPresent(read -> faults.add(MISSPELLED_KEY.formatted(excerptOf(attribute.getKey()), read))));

		ofNullable(valueOf(present, PROCESS_KEY_ATTRIBUTE))
			.map(String::trim)
			.filter(ProcessRules::isOversized)
			.ifPresent(key -> faults.add(OVERSIZED_KEY.formatted(key.length(), PROCESS_KEY_LENGTH)));

		present.stream()
			.filter(attribute -> PROCESS_BLOCKED_ATTRIBUTE.equals(attribute.getKey()))
			.map(LabelAttribute::getValue)
			.filter(Objects::nonNull)
			.filter(blocked -> !BLOCKED_VALUES.contains(blocked))
			.findFirst()
			.ifPresent(blocked -> faults.add(UNKNOWN_BLOCKED.formatted(excerptOf(blocked), BLOCKED_VALUES)));

		final var startMode = valueOf(present, PROCESS_START_MODE_ATTRIBUTE);

		if (isNull(startMode)) {
			return;
		}

		if (!EnumUtils.isValidEnum(ProcessStartMode.class, startMode)) {
			faults.add(UNKNOWN_START_MODE.formatted(excerptOf(startMode), START_MODES));
		}

		if (isNull(valueOf(present, PROCESS_KEY_ATTRIBUTE))) {
			faults.add(START_MODE_WITHOUT_KEY);
		}
	}

	/**
	 * Whether a key equals an attribute read by the service when case and surrounding blanks are ignored, without being
	 * spelled exactly like it.
	 */
	private static boolean isMisspelled(final String key, final String read) {
		return !read.equals(key) && Strings.CI.equals(key.strip(), read);
	}

	private static String valueOf(final List<LabelAttribute> attributes, final String key) {
		return attributes.stream()
			.filter(attribute -> key.equals(attribute.getKey()))
			.map(LabelAttribute::getValue)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

}
