package se.sundsvall.supportmanagement.api.validation.impl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import se.sundsvall.supportmanagement.api.validation.ValidTypeTag;
import se.sundsvall.supportmanagement.service.TagService;

public class ValidTypeTagConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidTypeTag, String> {
	@Autowired
	private TagService tagService;

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return isValid(value, tagService.findAllTypeTags(
			getPathVariable(PATHVARIABLE_NAMESPACE),
			getPathVariable(PATHVARIABLE_MUNICIPALITY_ID),
			"CATEGORY-1"), context); // TODO: Refactor when API is changed in UF-4537
	}
}
