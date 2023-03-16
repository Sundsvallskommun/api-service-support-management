package se.sundsvall.supportmanagement.api.validation;

import se.sundsvall.supportmanagement.api.validation.impl.ValidTypeTagConstraintValidator;

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
@Constraint(validatedBy = ValidTypeTagConstraintValidator.class)
public @interface ValidTypeTag {

	String message() default "not a valid typeTag";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
