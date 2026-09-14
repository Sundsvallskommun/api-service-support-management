package se.sundsvall.supportmanagement.integration.pwalkt.configuration;

import java.util.List;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Import(FeignConfiguration.class)
public class PwAlktConfiguration {

	public static final String CLIENT_ID = "pw-alkt";

	/**
	 * Lets a 422 through as itself rather than as a bad gateway. It is the one answer that refuses an event for good, and
	 * the relay has to be able to tell it apart from everything that is worth trying again.
	 */
	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final ClientRegistrationRepository clientRepository, final PwAlktProperties pwAlktProperties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID, List.of(UNPROCESSABLE_CONTENT.value())))
			.withRequestTimeoutsInSeconds(pwAlktProperties.connectTimeout(), pwAlktProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
