package se.sundsvall.supportmanagement.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.supportmanagement.api.validation.impl.ValidEventFilterConstraintValidator;

/**
 * A filter on a label being added must be one on the LABEL subtype, as only LABEL events carry the label they added. A
 * labelId on any other subtype would match nothing.
 */
@Documented
@Target({
	ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEventFilterConstraintValidator.class)
public @interface ValidEventFilter {

	String message() default "labelId may only be set when subtype is LABEL";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
