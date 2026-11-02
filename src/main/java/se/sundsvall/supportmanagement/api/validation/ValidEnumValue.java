package se.sundsvall.supportmanagement.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.supportmanagement.api.validation.impl.ValidEnumValueConstraintValidator;

/**
 * Holds a string field to the values of an enum without publishing the enum itself. The field stays a string in the
 * schema, and a value outside the enum is refused as a bad request.
 */
@Documented
@Target({
	ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEnumValueConstraintValidator.class)
public @interface ValidEnumValue {

	Class<? extends Enum<?>> value();

	String message() default "not a valid value";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
