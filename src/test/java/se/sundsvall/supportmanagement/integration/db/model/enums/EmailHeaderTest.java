package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.AUTO_SUBMITTED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.CONTENT_TYPE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.IN_REPLY_TO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.MESSAGE_ID;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.REFERENCES;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.RETURN_PATH;

class EmailHeaderTest {

	@Test
	void enums() {
		assertThat(EmailHeader.values()).containsExactlyInAnyOrder(IN_REPLY_TO, REFERENCES, MESSAGE_ID, AUTO_SUBMITTED, RETURN_PATH, CONTENT_TYPE);
	}

	@Test
	void enumValues() {
		assertThat(IN_REPLY_TO.getName()).isEqualTo("In-Reply-To");
		assertThat(REFERENCES.getName()).isEqualTo("References");
		assertThat(MESSAGE_ID.getName()).isEqualTo("Message-ID");
		assertThat(AUTO_SUBMITTED.getName()).isEqualTo("Auto-Submitted");
		assertThat(RETURN_PATH.getName()).isEqualTo("Return-Path");
		assertThat(CONTENT_TYPE.getName()).isEqualTo("Content-Type");
	}

}
