package se.sundsvall.supportmanagement.api.validation.impl;

import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.validation.ValidStatus;
import se.sundsvall.supportmanagement.service.MetadataService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class ValidStatusConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidStatus, String> {
	@Autowired
	private MetadataService metadataService;

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return isValid(value, getStatusNames(), context);
	}

	private List<String> getStatusNames() {
		return ofNullable(metadataService.findStatuses(getPathVariable(PATHVARIABLE_NAMESPACE), getPathVariable(PATHVARIABLE_MUNICIPALITY_ID))).orElse(emptyList()).stream()
			.map(Status::getName)
			.toList();
	}
}
