package se.sundsvall.supportmanagement.api.validation.impl;

import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.api.validation.ValidClassification;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.MetadataService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class ValidClassificationConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidClassification, Classification> {
	@Autowired
	private MetadataService metadataService;

	@Override
	public boolean isValid(Classification classification, ConstraintValidatorContext context) {
		if (metadataService.isValidated(getPathVariable(PATHVARIABLE_NAMESPACE), getPathVariable(PATHVARIABLE_MUNICIPALITY_ID), EntityType.CATEGORY)) {
			if (classification == null) {
				return true;
			}
			return isValid(classification.getCategory(), getCategoryNames(), context) &&
				isValid(classification.getType(), getTypeNames(classification.getCategory()), context);
		}
		return true;
	}

	private List<String> getCategoryNames() {
		return ofNullable(metadataService.findCategories(getPathVariable(PATHVARIABLE_NAMESPACE), getPathVariable(PATHVARIABLE_MUNICIPALITY_ID))).orElse(emptyList()).stream()
			.map(Category::getName)
			.toList();
	}

	private List<String> getTypeNames(String category) {
		return ofNullable(metadataService.findTypes(getPathVariable(PATHVARIABLE_NAMESPACE), getPathVariable(PATHVARIABLE_MUNICIPALITY_ID), category)).orElse(emptyList()).stream()
			.map(Type::getName)
			.toList();
	}
}
