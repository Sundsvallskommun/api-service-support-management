package se.sundsvall.supportmanagement.integration.messageexchange.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.messageexchange")
public record MessageExchangeProperties(int connectTimeout, int readTimeout) {
}
