package se.sundsvall.supportmanagement.config.masking;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;

@Configuration
class PayloadMaskingConfig {

	/**
	 * Collected by dept44's Logbook configuration along with every other {@link BodyFilter} bean, and applied after the
	 * filters it builds from {@code logbook.body-filters} itself.
	 */
	@Bean
	BodyFilter payloadMaskingBodyFilter(final PayloadMaskingProperties properties) {
		return new PayloadMaskingBodyFilter(properties);
	}
}
