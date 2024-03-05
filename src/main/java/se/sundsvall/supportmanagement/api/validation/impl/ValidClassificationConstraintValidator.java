package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.api.validation.ValidClassification;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.MetadataService;

public class ValidClassificationConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidClassification, Classification> {

	private final MetadataService metadataService;

	public ValidClassificationConstraintValidator(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	@Override
	public boolean isValid(Classification classification, ConstraintValidatorContext context) {
		if (classification == null) {
			return true;
		}

		final var namespace = getPathVariable(PATHVARIABLE_NAMESPACE);
		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);

		final var categoryNames = getCategoryNames(namespace, municipalityId);

		if (!isCategoryValid(namespace, municipalityId, categoryNames, classification, context)) {
			return false;
		}

		return isTypeValid(namespace, municipalityId, classification, context);
	}

	private boolean isCategoryValid(String namespace, String municipalityId, List<String> categoryNames, Classification classification, ConstraintValidatorContext context) {
		return !metadataService.isValidated(namespace, municipalityId, EntityType.CATEGORY) ||
			isValid(classification.getCategory(), categoryNames, context);
	}

	private boolean isTypeValid(String namespace, String municipalityId, Classification classification, ConstraintValidatorContext context) {
		return !metadataService.isValidated(namespace, municipalityId, EntityType.TYPE) ||
			isValid(classification.getType(), getTypeNames(namespace, municipalityId, classification.getCategory()), context);
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
