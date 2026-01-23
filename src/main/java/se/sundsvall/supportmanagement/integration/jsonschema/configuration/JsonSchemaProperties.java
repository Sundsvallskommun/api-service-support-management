package se.sundsvall.supportmanagement.integration.jsonschema.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the JSON Schema integration.
 *
 * @param connectTimeout the connection timeout in seconds
 * @param readTimeout    the read timeout in seconds
 */
@ConfigurationProperties("integration.json-schema")
public record JsonSchemaProperties(int connectTimeout, int readTimeout) {
}
