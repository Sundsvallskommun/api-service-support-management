package se.sundsvall.supportmanagement.integration.db.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum EmailHeader {
	IN_REPLY_TO("In-Reply-To"),
	REFERENCES("References"),
	MESSAGE_ID("Message-ID"),
	AUTO_SUBMITTED("Auto-Submitted"),
	RETURN_PATH("Return-Path"),
	CONTENT_TYPE("Content-Type");

	private final String name;

	EmailHeader(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
