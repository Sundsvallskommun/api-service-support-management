package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.messageexchange.Identifier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class IdentifierMapperTest {

	@ParameterizedTest
	@MethodSource("resolveChannelArguments")
	void resolveChannel(final String type, final String expectedChannel) {
		final var identifier = new Identifier().type(type).value("anyValue");

		assertThat(IdentifierMapper.resolveChannel(identifier)).isEqualTo(expectedChannel);
	}

	private static Stream<Arguments> resolveChannelArguments() {
		return Stream.of(
			Arguments.of("adAccount", Channels.WEB_UI),
			Arguments.of("partyId", Channels.MY_PAGES),
			Arguments.of("unknown", null),
			Arguments.of(null, null));
	}

	@Test
	void resolveChannelWithNullIdentifier() {
		assertThat(IdentifierMapper.resolveChannel(null)).isNull();
	}
}
