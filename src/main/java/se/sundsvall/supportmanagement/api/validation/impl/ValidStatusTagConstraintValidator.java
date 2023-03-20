package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.validation.ValidStatusTag;
import se.sundsvall.supportmanagement.service.MetadataService;

public class ValidStatusTagConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidStatusTag, String> {
	@Autowired
	private MetadataService tagService;

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return isValid(value, getStatusNames(), context);
	}

	private List<String> getStatusNames() {
		return ofNullable(tagService.findStatuses(getPathVariable(PATHVARIABLE_NAMESPACE), getPathVariable(PATHVARIABLE_MUNICIPALITY_ID))).orElse(emptyList()).stream()
			.map(Status::getName)
			.toList();
	}
}
