package se.sundsvall.supportmanagement.integration.db;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.BOOLEAN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.STRING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_ACCESS_CONTROL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_DISPLAY_NAME;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFY_REPORTER;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SHORT_CODE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class NamespaceConfigRepositoryTest {

	@Autowired
	private NamespaceConfigRepository repository;

	@Test
	void findByNamespaceAndMunicipalityId() {
		final var result = repository.findByNamespaceAndMunicipalityId("namespace-1", "2281");
		assertThat(result).isPresent();
	}

	@Test
	void existsByNamespaceAndMunicipalityId() {
		assertThat(repository.existsByNamespaceAndMunicipalityId("NAMEspace-1", "2281")).isTrue();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-99", "2289")).isFalse();
	}

	@Test
	void findAllByMunicipalityId() {
		final var municipalityId = "2281";
		final var result = repository.findAllByMunicipalityId(municipalityId);

		assertThat(result).hasSize(2).satisfiesExactlyInAnyOrder(bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(bean.getNamespace()).isEqualTo("namespace-1");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_DISPLAY_NAME)).isEqualTo("display name 1");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_SHORT_CODE)).isEqualTo("short_code-1");
			assertThat((Integer) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFICATION_TTL_IN_DAYS)).isEqualTo(10);
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_ACCESS_CONTROL)).isFalse();
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFY_REPORTER)).isFalse();
		}, bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(bean.getNamespace()).isEqualTo("namespace-3");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_DISPLAY_NAME)).isEqualTo("display name 3");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_SHORT_CODE)).isEqualTo("short_code-3");
			assertThat((Integer) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFICATION_TTL_IN_DAYS)).isEqualTo(30);
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_ACCESS_CONTROL)).isTrue();
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFY_REPORTER)).isTrue();
		});
	}

	@Test
	void findAll() {
		final var result = repository.findAll();

		assertThat(result).hasSize(3).satisfiesExactlyInAnyOrder(bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo("2281");
			assertThat(bean.getNamespace()).isEqualTo("namespace-1");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_DISPLAY_NAME)).isEqualTo("display name 1");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_SHORT_CODE)).isEqualTo("short_code-1");
			assertThat((Integer) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFICATION_TTL_IN_DAYS)).isEqualTo(10);
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_ACCESS_CONTROL)).isFalse();
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFY_REPORTER)).isFalse();
		}, bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo("2282");
			assertThat(bean.getNamespace()).isEqualTo("namespace-2");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_DISPLAY_NAME)).isEqualTo("display name 2");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_SHORT_CODE)).isEqualTo("short_code-2");
			assertThat((Integer) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFICATION_TTL_IN_DAYS)).isEqualTo(20);
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_ACCESS_CONTROL)).isFalse();
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFY_REPORTER)).isFalse();
		}, bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo("2281");
			assertThat(bean.getNamespace()).isEqualTo("namespace-3");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_DISPLAY_NAME)).isEqualTo("display name 3");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_SHORT_CODE)).isEqualTo("short_code-3");
			assertThat((Integer) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFICATION_TTL_IN_DAYS)).isEqualTo(30);
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_ACCESS_CONTROL)).isTrue();
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFY_REPORTER)).isTrue();
		});
	}

	@Test
	void create() {

		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var displayName = "displayName";
		final var shortCode = "shortCode";
		final var notificationTTLInDays = 40;
		final var toggleValue = true;

		final var entity = NamespaceConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create()
					.withKey(PROPERTY_DISPLAY_NAME)
					.withType(STRING)
					.withValue(displayName),
				NamespaceConfigValueEmbeddable.create()
					.withKey(PROPERTY_SHORT_CODE)
					.withType(STRING)
					.withValue(shortCode),
				NamespaceConfigValueEmbeddable.create()
					.withKey(PROPERTY_NOTIFICATION_TTL_IN_DAYS)
					.withType(INTEGER)
					.withValue(String.valueOf(notificationTTLInDays)),
				NamespaceConfigValueEmbeddable.create()
					.withKey(PROPERTY_ACCESS_CONTROL)
					.withType(BOOLEAN)
					.withValue(String.valueOf(toggleValue)),
				NamespaceConfigValueEmbeddable.create()
					.withKey(PROPERTY_NOTIFY_REPORTER)
					.withType(BOOLEAN)
					.withValue(String.valueOf(toggleValue))));

		assertThat(repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isFalse();

		repository.saveAndFlush(entity);

		final var result = repository.findByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(result).isPresent().get().satisfies(bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(bean.getNamespace()).isEqualTo(namespace);
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_DISPLAY_NAME)).isEqualTo(displayName);
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_SHORT_CODE)).isEqualTo(shortCode);
			assertThat((Integer) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFICATION_TTL_IN_DAYS)).isEqualTo(notificationTTLInDays);
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_ACCESS_CONTROL)).isEqualTo(toggleValue);
			assertThat((Boolean) ConfigPropertyExtractor.getValue(bean, PROPERTY_NOTIFY_REPORTER)).isEqualTo(toggleValue);
			assertThat(bean.getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
			assertThat(bean.getModified()).isNull();
		});
	}

	@Test
	void update() {
		final var entity = repository.findByNamespaceAndMunicipalityId("namespace-1", "2281")
			.orElseThrow(() -> new RuntimeException("Missing data in /db/scripts/testdata-junit.sql"));

		assertThat((String) ConfigPropertyExtractor.getValue(entity, PROPERTY_DISPLAY_NAME)).isEqualTo("display name 1");
		assertThat((String) ConfigPropertyExtractor.getValue(entity, PROPERTY_SHORT_CODE)).isEqualTo("short_code-1");

		repository.saveAndFlush(entity
			.withValues(new ArrayList<>(List.of(
				NamespaceConfigValueEmbeddable.create()
					.withKey(PROPERTY_DISPLAY_NAME)
					.withType(STRING)
					.withValue("new displayname"),
				NamespaceConfigValueEmbeddable.create()
					.withKey(PROPERTY_SHORT_CODE)
					.withType(STRING)
					.withValue("newCode")))));

		final var modifiedEntity = repository.findByNamespaceAndMunicipalityId("namespace-1", "2281");
		assertThat(modifiedEntity).isPresent().get().satisfies(bean -> {
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_DISPLAY_NAME)).isEqualTo("new displayname");
			assertThat((String) ConfigPropertyExtractor.getValue(bean, PROPERTY_SHORT_CODE)).isEqualTo("newCode");
			assertThat(bean.getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		});
	}

	@Test
	void delete() {
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "2281")).isTrue();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-2", "2282")).isTrue();

		repository.deleteByNamespaceAndMunicipalityId("namespace-1", "2281");

		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "2281")).isFalse();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-2", "2282")).isTrue();
	}
}
