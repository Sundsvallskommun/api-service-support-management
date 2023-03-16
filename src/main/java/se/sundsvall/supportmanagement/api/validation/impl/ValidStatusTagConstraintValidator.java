package se.sundsvall.supportmanagement.api.validation.impl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import se.sundsvall.supportmanagement.api.validation.ValidStatusTag;
import se.sundsvall.supportmanagement.service.TagService;

public class ValidStatusTagConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidStatusTag, String> {
	@Autowired
	private TagService tagService;

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return isValid(value, tagService.findAllStatusTags(
			getPathVariable(PATHVARIABLE_NAMESPACE),
			getPathVariable(PATHVARIABLE_MUNICIPALITY_ID)), context);
	}
}
