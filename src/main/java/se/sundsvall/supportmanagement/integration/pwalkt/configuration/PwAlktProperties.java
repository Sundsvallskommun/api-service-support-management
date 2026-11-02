package se.sundsvall.supportmanagement.integration.pwalkt.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.pw-alkt")
public record PwAlktProperties(int connectTimeout, int readTimeout) {
}
