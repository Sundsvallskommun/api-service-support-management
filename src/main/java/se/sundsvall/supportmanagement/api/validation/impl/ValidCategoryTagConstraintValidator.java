package se.sundsvall.supportmanagement.api.validation.impl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import se.sundsvall.supportmanagement.api.validation.ValidCategoryTag;
import se.sundsvall.supportmanagement.service.TagService;

public class ValidCategoryTagConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidCategoryTag, String> {
	@Autowired
	private TagService tagService;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return isValid(value, tagService.findAllCategoryTags(
			getPathVariable(PATHVARIABLE_NAMESPACE),
			getPathVariable(PATHVARIABLE_MUNICIPALITY_ID)), context);
	}
}
