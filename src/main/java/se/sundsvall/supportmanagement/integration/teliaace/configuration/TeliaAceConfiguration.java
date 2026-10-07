package se.sundsvall.supportmanagement.integration.teliaace.configuration;

import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

@Import(FeignConfiguration.class)
public class TeliaAceConfiguration {

	public static final String CLIENT_ID = "telia-ace";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final TeliaAceProperties teliaAceProperties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestTimeoutsInSeconds(teliaAceProperties.connectTimeout(), teliaAceProperties.readTimeout())
			.withRequestInterceptor(new BasicAuthRequestInterceptor(teliaAceProperties.username(), teliaAceProperties.password()))
			.composeCustomizersToOne();
	}
}
