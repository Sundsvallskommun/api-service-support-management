package se.sundsvall.supportmanagement.integration.teliaace.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.telia-ace")
public record TeliaAceProperties(int connectTimeout, int readTimeout, String username, String password) {
}
