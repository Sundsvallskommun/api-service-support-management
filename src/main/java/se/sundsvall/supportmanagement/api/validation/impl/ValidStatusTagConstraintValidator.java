package se.sundsvall.supportmanagement.api.validation.impl;

import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.api.validation.ValidStatusTag;
import se.sundsvall.supportmanagement.service.TagService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidStatusTagConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidStatusTag, String> {
	@Autowired
	private TagService tagService;

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return isValid(value, tagService.findAllStatusTags(), context);
	}
}
