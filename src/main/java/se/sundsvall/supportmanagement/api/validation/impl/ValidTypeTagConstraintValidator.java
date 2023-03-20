package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.api.validation.ValidTypeTag;
import se.sundsvall.supportmanagement.service.MetadataService;

public class ValidTypeTagConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidTypeTag, String> {
	@Autowired
	private MetadataService tagService;

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return isValid(value, getTypeNames("CATEGORY-1"), context); // TODO: Refactor in UF-4594
	}

	private List<String> getTypeNames(String category) {
		return ofNullable(tagService.findTypes(getPathVariable(PATHVARIABLE_NAMESPACE), getPathVariable(PATHVARIABLE_MUNICIPALITY_ID), category)).orElse(emptyList()).stream()
			.map(Type::getName)
			.toList();
	}
}
