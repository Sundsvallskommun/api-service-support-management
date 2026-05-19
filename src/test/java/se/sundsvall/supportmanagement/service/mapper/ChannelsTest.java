package se.sundsvall.supportmanagement.service.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelsTest {

	@Test
	void verifyChannelConstants() {
		assertThat(Channels.ESERVICE).isEqualTo("ESERVICE");
		assertThat(Channels.EMAIL).isEqualTo("EMAIL");
		assertThat(Channels.WEB_UI).isEqualTo("WEB_UI");
		assertThat(Channels.MY_PAGES).isEqualTo("MY_PAGES");
	}
}
