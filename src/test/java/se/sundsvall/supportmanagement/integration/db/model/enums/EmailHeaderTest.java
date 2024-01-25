package se.sundsvall.supportmanagement.integration.db.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.IN_REPLY_TO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.MESSAGE_ID;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.REFERENCES;

import org.junit.jupiter.api.Test;

class EmailHeaderTest {

	@Test
	void enums() {
		assertThat(EmailHeader.values()).containsExactlyInAnyOrder(IN_REPLY_TO, REFERENCES, MESSAGE_ID);
	}

	@Test
	void enumValues() {
		assertThat(IN_REPLY_TO.getName()).isEqualTo("In-Reply-To");
		assertThat(REFERENCES.getName()).isEqualTo("References");
		assertThat(MESSAGE_ID.getName()).isEqualTo("Message-ID");
	}

}
