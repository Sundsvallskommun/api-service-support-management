package se.sundsvall.supportmanagement.api.validation;

import se.sundsvall.supportmanagement.api.validation.impl.ValidClientIdTagConstraintValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidClientIdTagConstraintValidator.class)
public @interface ValidClientIdTag {

	String message() default "not a valid clientIdTag";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
