package se.sundsvall.supportmanagement.integration.messageexchange.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.messageexchange.configuration.MessageExchangeConfiguration.CLIENT_ID;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;
import se.sundsvall.dept44.support.Identifier;

@ExtendWith(MockitoExtension.class)
class MessageExchangeConfigurationTest {

	@Mock
	private ClientRegistrationRepository clientRegistrationRepositoryMock;

	@Mock
	private ClientRegistration clientRegistrationMock;

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Mock
	private MessageExchangeProperties propertiesMock;

	@Test
	void testFeignBuilderCustomizer() {
		final var configuration = new MessageExchangeConfiguration();

		when(clientRegistrationRepositoryMock.findByRegistrationId(any())).thenReturn(clientRegistrationMock);
		when(propertiesMock.connectTimeout()).thenReturn(1);
		when(propertiesMock.readTimeout()).thenReturn(2);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			var customizer = configuration.feignBuilderCustomizer(propertiesMock, clientRegistrationRepositoryMock);

			ArgumentCaptor<ProblemErrorDecoder> errorDecoderCaptor = ArgumentCaptor.forClass(ProblemErrorDecoder.class);

			verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
			verify(clientRegistrationRepositoryMock).findByRegistrationId(CLIENT_ID);
			verify(feignMultiCustomizerSpy).withRetryableOAuth2InterceptorForClientRegistration(same(clientRegistrationMock));
			verify(propertiesMock).connectTimeout();
			verify(propertiesMock).readTimeout();
			verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(1, 2);
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(customizer).isSameAs(feignBuilderCustomizerMock);
		}
	}

	@ParameterizedTest
	@MethodSource("toValidUuidsStreamArguments")
	void createSentByHeaderValue(Identifier identifier, String expectedHeaderValue) {

		// Arrange
		final var configuration = new MessageExchangeConfiguration();

		// Act
		final var result = configuration.createSentByHeaderValue(identifier);

		// Assert
		assertThat(result).isEqualTo(expectedHeaderValue);
	}

	private static Stream<Arguments> toValidUuidsStreamArguments() {
		return Stream.of(
			Arguments.of(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"), "joe01doe; type=adAccount"),
			Arguments.of(Identifier.create().withType(Identifier.Type.PARTY_ID).withValue("98c7b451-a14a-4f9f-91da-8834ba01eb81"), "98c7b451-a14a-4f9f-91da-8834ba01eb81; type=partyId"),
			Arguments.of(Identifier.create(), null),
			Arguments.of(Identifier.create().withType(null).withValue("joe01doe"), null),
			Arguments.of(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(null), null));
	}
}
