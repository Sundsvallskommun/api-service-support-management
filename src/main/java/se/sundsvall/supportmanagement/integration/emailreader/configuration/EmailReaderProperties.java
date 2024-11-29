package se.sundsvall.supportmanagement.integration.emailreader.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.emailreader")

public record EmailReaderProperties(int connectTimeout, int readTimeout, String namespace,
	String municipalityId, String errandClosedEmailTemplate,
	String errandClosedEmailSender) {

}
