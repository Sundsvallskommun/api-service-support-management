package se.sundsvall.supportmanagement.integration.teliaace.configuration;

import feign.auth.BasicAuthRequestInterceptor;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Import(FeignConfiguration.class)
public class TeliaAceConfiguration {

	public static final String CLIENT_ID = "telia-ace";

	private static final Logger LOG = LoggerFactory.getLogger(TeliaAceConfiguration.class);

	/**
	 * The feature is opt-in per namespace, but {@code TeliaAceClient} is still built at startup, so a missing username
	 * or password must not throw - {@code BasicAuthRequestInterceptor} does on a null value - and take the whole
	 * service down in an environment where the feature is simply not configured yet.
	 */
	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final TeliaAceProperties teliaAceProperties) {
		// Without the bypass list, ProblemErrorDecoder normalizes every 4xx status to BAD_GATEWAY on the thrown
		// ClientProblem, so TeliaAceWorkItemWorker couldn't otherwise tell an auth/rate-limit failure apart from
		// Telia ACE rejecting a work item it can no longer act on.
		final var customizer = FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID, List.of(UNAUTHORIZED.value(), FORBIDDEN.value(), TOO_MANY_REQUESTS.value())))
			.withRequestTimeoutsInSeconds(teliaAceProperties.connectTimeout(), teliaAceProperties.readTimeout());

		if (StringUtils.isAnyBlank(teliaAceProperties.username(), teliaAceProperties.password())) {
			LOG.warn("No username/password configured for the '{}' integration - calls to it will fail until it is", CLIENT_ID);
		} else {
			customizer.withRequestInterceptor(new BasicAuthRequestInterceptor(teliaAceProperties.username(), teliaAceProperties.password()));
		}

		return customizer.composeCustomizersToOne();
	}
}
