package se.sundsvall.supportmanagement.api.validation;

import se.sundsvall.supportmanagement.api.validation.impl.UniqueExternalTagKeysConstraintValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueExternalTagKeysConstraintValidator.class)
public @interface UniqueExternalTagKeys {

	String message() default "keys in the collection must be unique";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
