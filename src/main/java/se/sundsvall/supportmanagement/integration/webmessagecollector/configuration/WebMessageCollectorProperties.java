package se.sundsvall.supportmanagement.integration.webmessagecollector.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "integration.web-message-collector")
public record WebMessageCollectorProperties(int connectTimeout, int readTimeout,
                                            List<String> familyIds) {

}
