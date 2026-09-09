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
 * Holds a string field to the values of an enum without publishing the enum itself.
 * <p>
 * A field typed as an enum becomes an enum in the schema, and a client that generated one from it throws the day a
 * value
 * is added rather than ignoring what it does not know. Keeping the field a string and naming the set here leaves the
 * wire open and the service closed: an unknown value is still refused, and refused as the bad request it is.
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
