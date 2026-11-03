package se.sundsvall.supportmanagement.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.supportmanagement.api.validation.impl.ValidEmailRecipientsConstraintValidator;

@Documented
@Target({
	ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEmailRecipientsConstraintValidator.class)
public @interface ValidEmailRecipients {

	String message() default "at least one of recipient and recipients must be set";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
