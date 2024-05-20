package se.sundsvall.supportmanagement.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import se.sundsvall.supportmanagement.api.validation.impl.ValidSuspendConstraintValidator;

@Documented
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSuspendConstraintValidator.class)
public @interface ValidSuspend {

	String message() default "to date must be after from date";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
