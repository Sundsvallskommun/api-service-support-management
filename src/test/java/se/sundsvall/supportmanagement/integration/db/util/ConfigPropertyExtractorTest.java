package se.sundsvall.supportmanagement.integration.db.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.BOOLEAN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.STRING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_ACCESS_CONTROL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SHORT_CODE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;

class ConfigPropertyExtractorTest {

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void testExtractBoolean(boolean value) {
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey("access_control").withValue(String.valueOf(value)).withType(BOOLEAN)));

		final var result = ConfigPropertyExtractor.getOptionalValue(namespaceConfig, PROPERTY_ACCESS_CONTROL);

		assertThat(result)
			.isEqualTo(value)
			.isInstanceOf(Boolean.class);
	}

	@Test
	void testExtractString() {
		final var value = "value";
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey("short_code").withValue(String.valueOf(value)).withType(STRING)));

		final var result = ConfigPropertyExtractor.getOptionalValue(namespaceConfig, PROPERTY_SHORT_CODE);

		assertThat(result)
			.isEqualTo(value)
			.isInstanceOf(String.class);
	}

	@Test
	void testExtractNumeric() {
		final var value = 123456;
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey("notification_ttl_in_days").withValue(String.valueOf(value)).withType(INTEGER)));

		final var result = ConfigPropertyExtractor.getOptionalValue(namespaceConfig, PROPERTY_NOTIFICATION_TTL_IN_DAYS);

		assertThat(result)
			.isEqualTo(value)
			.isInstanceOf(Integer.class);
	}

	@Test
	void testExtractOptionalWithNoMatch() {
		final var result = ConfigPropertyExtractor.getOptionalValue(NamespaceConfigEntity.create(), PROPERTY_SHORT_CODE);

		assertThat(result).isNull();
	}

	@Test
	void testExtractRequiredWithNoMatch() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var namespaceConfig = NamespaceConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);

		final var e = assertThrows(ThrowableProblem.class, () -> ConfigPropertyExtractor.getRequiredValue(namespaceConfig, PROPERTY_SHORT_CODE));

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("No configurationproperty matching key 'SHORT_CODE' found in configuration for municipality 'municipalityId' and namespace 'namespace'");
	}

	@Test
	void testExtractRequiredWhenConfigMissing() {
		final var e = assertThrows(ThrowableProblem.class, () -> ConfigPropertyExtractor.getRequiredValue(null, PROPERTY_SHORT_CODE));

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("No configuration present");
	}

}
