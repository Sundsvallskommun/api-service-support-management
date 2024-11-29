package se.sundsvall.supportmanagement.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import se.sundsvall.supportmanagement.api.validation.impl.ValidMessageIdConstraintValidator;

@Documented
@Target({
	ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidMessageIdConstraintValidator.class)
public @interface ValidMessageId {

	String message() default "text is not valid message id format";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
