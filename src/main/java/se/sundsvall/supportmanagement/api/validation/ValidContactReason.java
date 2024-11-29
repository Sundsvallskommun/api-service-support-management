package se.sundsvall.supportmanagement.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import se.sundsvall.supportmanagement.api.validation.impl.ValidContactReasonConstraintValidator;

@Documented
@Target({
	ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidContactReasonConstraintValidator.class)
public @interface ValidContactReason {

	boolean nullable() default false;

	String message() default "not a valid contact reason";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
