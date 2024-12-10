package se.sundsvall.supportmanagement.integration.webmessagecollector.configuration;

import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

@Import(FeignConfiguration.class)
public class WebMessageCollectorConfiguration {

	public static final String CLIENT_ID = "web-message-collector";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final WebMessageCollectorProperties webMessageCollectorProperties, final ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.withRequestTimeoutsInSeconds(webMessageCollectorProperties.connectTimeout(), webMessageCollectorProperties.readTimeout())
			.composeCustomizersToOne();
	}
}
