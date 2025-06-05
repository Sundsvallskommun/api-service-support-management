package se.sundsvall.supportmanagement.integration.messageexchange.configuration;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;

import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.cloud.openfeign.support.JsonFormWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;
import se.sundsvall.dept44.support.Identifier;

@Import(FeignConfiguration.class)
public class MessageExchangeConfiguration {

	public static final String CLIENT_ID = "messageexchange";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(MessageExchangeProperties messageExchangeProperties, ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestInterceptor(builder -> builder.header(Identifier.HEADER_NAME, createSentByHeaderValue(Identifier.get())))
			.withRequestTimeoutsInSeconds(messageExchangeProperties.connectTimeout(), messageExchangeProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}

	@Bean
	JsonFormWriter jsonFormWriter() {
		// Needed for Feign to handle json objects sent as requestpart correctly
		return new JsonFormWriter();
	}

	String createSentByHeaderValue(Identifier identifier) {
		return ofNullable(identifier)
			.filter(i -> allNotNull(i.getType(), i.getValue()))
			.map(i -> "%s; type=%s".formatted(ofNullable(i.getValue()).orElse(""), ofNullable(i.getType()).map(t -> UPPER_UNDERSCORE.to(LOWER_CAMEL, i.getType().name())).orElse("")))
			.orElse(null);
	}
}
