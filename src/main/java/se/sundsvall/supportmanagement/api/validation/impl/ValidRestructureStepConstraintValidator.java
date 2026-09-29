package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;
import se.sundsvall.supportmanagement.api.validation.ValidRestructureStep;

/**
 * Checks that a {@link LabelRestructureStep} only carries the fields its {@code type} allows - e.g. a {@code DELETE}
 * step must not also carry a {@code displayName}, and a {@code MOVE} step must not carry {@code sourcePaths}. Each
 * step type's own required fields ({@code path}, {@code sourcePaths}) are still just plain {@code @NotEmpty} on the
 * field, since those apply to every step type that uses them the same way; what differs per type is only which
 * fields are allowed to be present at all.
 */
public class ValidRestructureStepConstraintValidator implements ConstraintValidator<ValidRestructureStep, LabelRestructureStep> {

	@Override
	public boolean isValid(final LabelRestructureStep step, final ConstraintValidatorContext context) {
		if ((step == null) || (step.getType() == null)) {
			// Left to @NotNull/@NotEmpty on the individual fields - nothing type-specific to check without a type.
			return true;
		}

		return switch (step.getType()) {
			case ADD -> StringUtils.hasText(step.getDisplayName())
				&& StringUtils.hasText(step.getClassification())
				&& isEmpty(step.getDestinationParentPath())
				&& isEmpty(step.getSourcePaths())
				&& (step.getNewResourceName() == null);
			case RENAME -> StringUtils.hasText(step.getDisplayName())
				&& (step.getClassification() == null)
				&& isEmpty(step.getDestinationParentPath())
				&& isEmpty(step.getSourcePaths())
				&& (step.getNewResourceName() == null);
			case DELETE -> (step.getDisplayName() == null)
				&& (step.getClassification() == null)
				&& isEmpty(step.getDestinationParentPath())
				&& isEmpty(step.getSourcePaths())
				&& (step.getNewResourceName() == null);
			case MOVE -> (step.getClassification() == null)
				&& isEmpty(step.getSourcePaths());
			case MERGE -> !CollectionUtils.isEmpty(step.getSourcePaths())
				&& step.getSourcePaths().stream().noneMatch(CollectionUtils::isEmpty)
				&& (step.getDisplayName() == null)
				&& (step.getClassification() == null)
				&& isEmpty(step.getDestinationParentPath())
				&& (step.getNewResourceName() == null);
		};
	}

	private static boolean isEmpty(final List<?> value) {
		return Objects.isNull(value) || value.isEmpty();
	}
}
