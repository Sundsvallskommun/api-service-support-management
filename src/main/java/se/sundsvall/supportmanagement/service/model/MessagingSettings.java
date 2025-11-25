package se.sundsvall.supportmanagement.service.model;

public record MessagingSettings(String supportText, String contactInformationUrl, String smsSender, String contactInformationEmail, String contactInformationEmailName) {
}
