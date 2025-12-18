package se.sundsvall.supportmanagement.integration.db.util;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.NOT_FOUND;

import org.apache.commons.lang3.Strings;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;

public class ConfigPropertyExtractor {
	private ConfigPropertyExtractor() {}

	public static final String PROPERTY_DISPLAY_NAME = "DISPLAY_NAME";
	public static final String PROPERTY_SHORT_CODE = "SHORT_CODE";
	public static final String PROPERTY_NOTIFICATION_TTL_IN_DAYS = "NOTIFICATION_TTL_IN_DAYS";
	public static final String PROPERTY_ACCESS_CONTROL = "ACCESS_CONTROL";

	/**
	 * Get the value for provided key as the type that is defined for the key/value-pair or null if no property matching
	 * provided key is found.
	 *
	 * @param  nullableNamespaceConfigEntity config entity to find property value in
	 * @param  key                           value of key to match
	 * @return                               value for property whos key is matching sent in key or null if no match is
	 *                                       found
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T getOptionalValue(NamespaceConfigEntity nullableNamespaceConfigEntity, String key) {
		return (T) ofNullable(nullableNamespaceConfigEntity).orElse(NamespaceConfigEntity.create())
			.getValues().stream()
			.filter(configValue -> Strings.CI.equals(key, configValue.getKey()))
			.map(ConfigPropertyExtractor::getAsTypedClass)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Get the value for provided key as the type that is defined for the key/value-pair. Method will throw exception if no
	 * property matching key is found.
	 *
	 * @param  namespaceConfigEntity config entity to find property value in
	 * @param  key                   value of key to match
	 * @return                       value for property whos key is matching sent in key
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T getRequiredValue(NamespaceConfigEntity namespaceConfigEntity, String key) {
		if (isNull(namespaceConfigEntity)) {
			throw Problem.valueOf(NOT_FOUND, "No configuration present");
		}

		return (T) ofNullable(getOptionalValue(namespaceConfigEntity, key))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No configurationproperty matching key '%s' found in configuration for municipality '%s' and namespace '%s'"
				.formatted(key, namespaceConfigEntity.getMunicipalityId(), namespaceConfigEntity.getNamespace())));
	}

	private static Object getAsTypedClass(NamespaceConfigValueEmbeddable configValue) {
		return switch (configValue.getType()) {
			case BOOLEAN -> Boolean.valueOf(configValue.getValue());
			case INTEGER -> Integer.valueOf(configValue.getValue());
			case STRING -> String.valueOf(configValue.getValue());
		};
	}
}
