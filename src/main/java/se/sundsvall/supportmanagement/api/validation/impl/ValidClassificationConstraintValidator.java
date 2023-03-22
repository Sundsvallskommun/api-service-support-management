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
		if (classification == null) {
			return true;
		}
		final var namespace = getPathVariable(PATHVARIABLE_NAMESPACE);
		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);

		if (metadataService.isValidated(namespace, municipalityId, EntityType.CATEGORY)) {
			return isValid(classification.getCategory(), getCategoryNames(namespace, municipalityId), context) &&
				isValid(classification.getType(), getTypeNames(namespace, municipalityId, classification.getCategory()), context);
		}
		return true;
	}

	private List<String> getCategoryNames(String namespace, String municipalityId) {
		return ofNullable(metadataService.findCategories(namespace, municipalityId)).orElse(emptyList()).stream()
			.map(Category::getName)
			.toList();
	}

	private List<String> getTypeNames(String namespace, String municipalityId, String category) {
		return ofNullable(metadataService.findTypes(namespace, municipalityId, category)).orElse(emptyList()).stream()
			.map(Type::getName)
			.toList();
	}
}
