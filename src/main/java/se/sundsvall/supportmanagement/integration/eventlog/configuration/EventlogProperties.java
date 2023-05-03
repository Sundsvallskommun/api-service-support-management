package se.sundsvall.supportmanagement.integration.eventlog.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.eventlog")
public record EventlogProperties(int connectTimeout, int readTimeout) {
}
