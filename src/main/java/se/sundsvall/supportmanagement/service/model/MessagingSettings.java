package se.sundsvall.supportmanagement.service.model;

public record MessagingSettings(String supportText, String reporterSupportText, String contactInformationUrl, String katlaUrl, String smsSender, String contactInformationEmail, String contactInformationEmailName) {
}
