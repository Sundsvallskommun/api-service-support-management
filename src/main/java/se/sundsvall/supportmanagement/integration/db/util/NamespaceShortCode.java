package se.sundsvall.supportmanagement.integration.db.util;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum NamespaceShortCode {

	KC("CONTACTCENTER", "2281");

	private final String namespace;

	private final String municipalityId;

	NamespaceShortCode(final String namespace, final String municipalityId) {
		this.namespace = namespace;
		this.municipalityId = municipalityId;
	}

	public static NamespaceShortCode findByNamespace(final String namespace, final String municipalityId) {
		return Arrays.stream(values())
			.filter(namespaceShortCode -> namespaceShortCode.namespace.equals(namespace))
			.filter(namespaceShortCode -> namespaceShortCode.municipalityId.equals(municipalityId))
			.findFirst()
			.orElseThrow(() -> new NoSuchElementException("No namespace shortcode found for namespace: "
				+ namespace + " and municipalityId: " + municipalityId));
	}
}
