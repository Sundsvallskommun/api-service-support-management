package se.sundsvall.supportmanagement.integration.accessmapper.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.accessmapper")
public record AccessMapperProperties(int connectTimeout, int readTimeout) {
}
