package se.sundsvall.supportmanagement.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.supportmanagement.api.validation.impl.ValidPurgeCutoffConstraintValidator;

/**
 * Guards the cutoff of a purge against being set too close to the present.
 * <p>
 * A purge cannot be undone and a mistyped timestamp is indistinguishable from a deliberate one by the time it reaches
 * the database. The floor is what stands between a slip of the keyboard and the loss of an entire namespace, so a
 * cutoff nearer than the configured minimum age is refused rather than merely discouraged.
 */
@Documented
@Target({
	ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPurgeCutoffConstraintValidator.class)
public @interface ValidPurgeCutoff {
	String message() default "is too close to the present to be used as a purge cutoff";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
