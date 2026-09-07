package se.sundsvall.supportmanagement.integration.db.util;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.BOOLEAN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.STRING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_ACCESS_CONTROL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_PROCESS_TRIGGER;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SHORT_CODE;

class ConfigPropertyExtractorTest {

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void testExtractBoolean(boolean value) {
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey("access_control").withValue(String.valueOf(value)).withType(BOOLEAN)));

		final var result = ConfigPropertyExtractor.getNullableValue(namespaceConfig, PROPERTY_ACCESS_CONTROL);

		assertThat(result)
			.isEqualTo(value);
	}

	@Test
	void testExtractString() {
		final var value = "value";
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey("short_code").withValue(String.valueOf(value)).withType(STRING)));

		final var result = ConfigPropertyExtractor.getNullableValue(namespaceConfig, PROPERTY_SHORT_CODE);

		assertThat(result)
			.isEqualTo(value);
	}

	@Test
	void testExtractNumeric() {
		final var value = 123456;
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey("notification_ttl_in_days").withValue(String.valueOf(value)).withType(INTEGER)));

		final var result = ConfigPropertyExtractor.getNullableValue(namespaceConfig, PROPERTY_NOTIFICATION_TTL_IN_DAYS);

		assertThat(result)
			.isEqualTo(value);
	}

	@Test
	void testExtractOptionalWithNoMatch() {
		final var result = ConfigPropertyExtractor.getNullableValue(NamespaceConfigEntity.create(), PROPERTY_SHORT_CODE);

		assertThat(result).isNull();
	}

	@Test
	void testExtractEveryValueForKey() {
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_PROCESS_TRIGGER).withValue("ERRAND").withType(STRING),
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_SHORT_CODE).withValue("NS1").withType(STRING),
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_PROCESS_TRIGGER).withValue("MESSAGE").withType(STRING),
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_PROCESS_TRIGGER).withValue("DECISION").withType(STRING)));

		final var result = ConfigPropertyExtractor.<String>getValues(namespaceConfig, PROPERTY_PROCESS_TRIGGER);

		// The single valued reader takes the first row and drops the rest, which is what this method exists to avoid
		assertThat((String) ConfigPropertyExtractor.getNullableValue(namespaceConfig, PROPERTY_PROCESS_TRIGGER)).isEqualTo("ERRAND");
		assertThat(result).containsExactly("ERRAND", "MESSAGE", "DECISION");
	}

	@Test
	void testExtractValuesOfTypeOtherThanString() {
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_NOTIFICATION_TTL_IN_DAYS).withValue("10").withType(INTEGER),
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_NOTIFICATION_TTL_IN_DAYS).withValue("20").withType(INTEGER)));

		final var result = ConfigPropertyExtractor.<Integer>getValues(namespaceConfig, PROPERTY_NOTIFICATION_TTL_IN_DAYS);

		assertThat(result).containsExactly(10, 20);
	}

	@Test
	void testExtractValuesWithNoMatch() {
		assertThat(ConfigPropertyExtractor.getValues(NamespaceConfigEntity.create(), PROPERTY_PROCESS_TRIGGER)).isEmpty();
	}

	@Test
	void testExtractValuesWhenConfigMissing() {
		assertThat(ConfigPropertyExtractor.getValues(null, PROPERTY_PROCESS_TRIGGER)).isEmpty();
	}

	@Test
	void testExtractRequiredWithNoMatch() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);

		final var e = assertThrows(ThrowableProblem.class, () -> ConfigPropertyExtractor.getValue(namespaceConfig, PROPERTY_SHORT_CODE));

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("No configurationproperty matching key 'SHORT_CODE' found in configuration for municipality 'municipalityId' and namespace 'namespace'");
	}

	@Test
	void testExtractRequiredWhenConfigMissing() {
		final var e = assertThrows(ThrowableProblem.class, () -> ConfigPropertyExtractor.getValue(null, PROPERTY_SHORT_CODE));

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("No configuration present");
	}

}
