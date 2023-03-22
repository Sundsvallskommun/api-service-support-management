package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.validation.ValidCategoryTag;
import se.sundsvall.supportmanagement.service.MetadataService;

public class ValidCategoryTagConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidCategoryTag, String> {
	@Autowired
	private MetadataService metadataService;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return isValid(value, getCategoryNames(), context);
	}

	private List<String> getCategoryNames() {
		return ofNullable(metadataService.findCategories(getPathVariable(PATHVARIABLE_NAMESPACE), getPathVariable(PATHVARIABLE_MUNICIPALITY_ID))).orElse(emptyList()).stream()
			.map(Category::getName)
			.toList();
	}
}
