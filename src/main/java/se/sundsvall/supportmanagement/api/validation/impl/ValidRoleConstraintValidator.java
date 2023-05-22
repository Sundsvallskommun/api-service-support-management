package se.sundsvall.supportmanagement.api.validation.impl;

import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.validation.ValidRole;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.MetadataService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class ValidRoleConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidRole, String> {
	@Autowired
	private MetadataService metadataService;

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		final var namespace = getPathVariable(PATHVARIABLE_NAMESPACE);
		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);
		return  !metadataService.isValidated(namespace, municipalityId, EntityType.ROLE) ||
			isValid(value, getRoles(namespace, municipalityId), context);
	}

	private List<String> getRoles(String namespace, String municipalityId) {
		return ofNullable(metadataService.findRoles(namespace, municipalityId)).orElse(emptyList()).stream()
			.map(Role::getName)
			.toList();
	}
}
