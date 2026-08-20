package se.sundsvall.supportmanagement.integration.accessmapper.configuration;

import java.util.List;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Import(FeignConfiguration.class)
public class AccessMapperConfiguration {

	public static final String CLIENT_ID = "accessmapper";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(AccessMapperProperties accessMapperProperties, ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			// A user unknown to the access mapper is answered with 404, which means "no grants" rather than a failure.
			// Bypassing it lets AccessMapperService fall through to an empty result instead of turning reads into 502s.
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value())))
			.withRequestTimeoutsInSeconds(accessMapperProperties.connectTimeout(), accessMapperProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
