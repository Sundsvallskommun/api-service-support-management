package se.sundsvall.supportmanagement.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_NAMESPACE;

import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.service.MetadataService;

@ExtendWith(MockitoExtension.class)
class ValidContactReasonConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private MetadataService metadataServiceMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidContactReasonConstraintValidator validator;

	private static Stream<Arguments> contactReasonArgumentProvider() {
		return Stream.of(
			Arguments.of("contactReason", true),
			Arguments.of("invalid", false));
	}

	@ParameterizedTest
	@MethodSource("contactReasonArgumentProvider")
	void validContactReason(final String reason, final boolean valid) {
		final var validReasons = List.of(ContactReason.create().withReason("contactReason"));
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (var requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.findContactReasons(namespace, municipalityId)).thenReturn(validReasons);

			assertThat(validator.isValid(reason, constraintValidatorContextMock)).isEqualTo(valid);
			verify(metadataServiceMock).findContactReasons(namespace, municipalityId);
		}

	}

}
