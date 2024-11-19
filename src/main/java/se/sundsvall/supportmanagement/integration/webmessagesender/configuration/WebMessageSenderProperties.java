package se.sundsvall.supportmanagement.integration.webmessagesender.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.web-message-sender")
public record WebMessageSenderProperties(int connectTimeout, int readTimeout) {
}
