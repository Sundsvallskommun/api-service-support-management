package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.messageexchange.Identifier;

import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.service.mapper.Channels.MY_PAGES;
import static se.sundsvall.supportmanagement.service.mapper.Channels.WEB_UI;

public final class IdentifierMapper {

	private IdentifierMapper() {}

	public static String resolveChannel(final Identifier createdBy) {
		return ofNullable(createdBy)
			.map(Identifier::getType)
			.map(IdentifierMapper::toChannel)
			.orElse(null);
	}

	private static String toChannel(final String type) {
		return switch (type) {
			case "adAccount" -> WEB_UI;
			case "partyId" -> MY_PAGES;
			default -> null;
		};
	}
}
