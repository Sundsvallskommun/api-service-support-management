package se.sundsvall.supportmanagement.api.validation.impl;

import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.api.validation.ValidCategoryTag;
import se.sundsvall.supportmanagement.service.TagService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidCategoryTagConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidCategoryTag, String> {
	@Autowired
	private TagService tagService;

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return isValid(value, tagService.findAllCategoryTags(), context);
	}
}
