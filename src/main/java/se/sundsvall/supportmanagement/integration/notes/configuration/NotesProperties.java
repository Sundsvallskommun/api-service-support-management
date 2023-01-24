package se.sundsvall.supportmanagement.integration.notes.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.notes")
public record NotesProperties(int connectTimeout, int readTimeout) {
}
