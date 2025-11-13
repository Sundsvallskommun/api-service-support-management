package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.MetadataService;

public abstract class ValidClassificationConstraintValidator extends AbstractTagConstraintValidator {

	private final MetadataService metadataService;
	private boolean nullableIfActive;

	public ValidClassificationConstraintValidator(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	protected void setNullableIfActive(boolean nullableIfActive) {
		this.nullableIfActive = nullableIfActive;
	}

	public boolean isValid(Classification classification, ConstraintValidatorContext context) {
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
			(nullableIfActive && classification == null) ||
			(classification != null && isValidAndNotBlank(classification.getCategory(), categoryNames, context));
	}

	private boolean isTypeValid(String namespace, String municipalityId, Classification classification, ConstraintValidatorContext context) {
		return !metadataService.isValidated(namespace, municipalityId, EntityType.TYPE) || (nullableIfActive && classification == null) ||
			(classification != null && isValidAndNotBlank(classification.getType(), getTypeNames(namespace, municipalityId, classification.getCategory()), context));
	}

	private List<String> getCategoryNames(String namespace, String municipalityId) {
		return ofNullable(metadataService.findCategories(namespace, municipalityId)).orElse(emptyList()).stream()
			.map(Category::getName)
			.toList();
	}

	private List<String> getTypeNames(String namespace, String municipalityId, String category) {
		return ofNullable(category).map(cat -> metadataService.findTypes(namespace, municipalityId, cat)).orElse(emptyList()).stream()
			.map(Type::getName)
			.toList();
	}

}
