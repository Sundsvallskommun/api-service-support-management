package se.sundsvall.supportmanagement.integration.jsonschema.configuration;

import java.util.List;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Configuration class for the JSON Schema integration.
 * Configures the Feign client with OAuth2 authentication, error handling, and request timeouts.
 */
@Import(FeignConfiguration.class)
public class JsonSchemaConfiguration {

	public static final String CLIENT_ID = "json-schema";

	/**
	 * Creates a customized Feign builder for the JSON Schema client.
	 *
	 * @param  jsonSchemaProperties         the configuration properties for timeouts
	 * @param  clientRegistrationRepository the repository for OAuth2 client registrations
	 * @return                              a configured FeignBuilderCustomizer
	 */
	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(JsonSchemaProperties jsonSchemaProperties, ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			// 404 is bypassed (kept as NOT_FOUND instead of being wrapped in BAD_GATEWAY) so that
			// HandoverPreviewService#isSchemaRegistered can tell "schema not registered" apart from a genuine upstream error.
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value())))
			.withRequestTimeoutsInSeconds(jsonSchemaProperties.connectTimeout(), jsonSchemaProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
