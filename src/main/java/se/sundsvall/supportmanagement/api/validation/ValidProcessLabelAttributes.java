package se.sundsvall.supportmanagement.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.supportmanagement.api.validation.impl.ValidProcessLabelAttributesConstraintValidator;

/**
 * Validates the label attributes the service reads itself - {@code processKey} and {@code processStartMode} -
 * throughout a label tree. The rules are listed on {@link ValidProcessLabelAttributesConstraintValidator}.
 */
@Documented
@Target({
	ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidProcessLabelAttributesConstraintValidator.class)
public @interface ValidProcessLabelAttributes {

	String message() default "the process attributes of a label are not valid";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
