package se.sundsvall.supportmanagement.integration.db.util;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum NamespaceShortCode {

	KC("CONTACTCENTER");

	private final String namespace;

	NamespaceShortCode(final String namespace) {
		this.namespace = namespace;
	}

	public static NamespaceShortCode findByNamespace(final String namespace) {
		return Arrays.stream(values())
			.filter(namespaceShortCode -> namespaceShortCode.namespace.equals(namespace))
			.findFirst()
			.orElseThrow(() -> new NoSuchElementException("No namespace shortcode found for " + namespace));
	}
}
