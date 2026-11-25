package se.sundsvall.supportmanagement.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.supportmanagement.api.validation.impl.ValidResolutionConstraintValidator;

/**
 * The resolution of an errand, held to the resolution codes registered for the namespace in the path once its namespace
 * config validates RESOLUTION_CODE, like the other metadata. Case is ignored, and a deprecated code is accepted: being
 * deprecated only tells a client not to offer the code. An empty value is left alone, since it clears the resolution.
 */
@Documented
@Target({
	ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidResolutionConstraintValidator.class)
public @interface ValidResolution {

	String message() default "not a valid resolution";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
