package se.sundsvall.supportmanagement;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.ErrandDataDeleter;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that every table pointing at an errand is taken with it when the errand is removed, by the single delete
 * as well as by the retention purge.
 * <p>
 * A table is covered when the database cascades it, when {@link ErrandEntity} maps it so that JPA cascades it, or when
 * {@link ErrandDataDeleter} removes the rows itself.
 * <p>
 * The test reads the schema the entities generate, so a table added by a migration alone is invisible to it.
 */
class ErrandRemovalCoverageTest {

	private static final String SCHEMA = "db/scripts/schema.sql";

	/**
	 * Constraints on the errand table, and whether the database takes the rows with it. Read from the schema with the
	 * whitespace flattened.
	 */
	private static final Pattern FOREIGN_KEY_TO_ERRAND = Pattern.compile(
		"alter table (?:if exists )?(\\w+) add constraint \\w+ foreign key \\(\\w+\\) references errand \\(id\\)( on delete cascade)?;");

	/**
	 * Tables {@link ErrandDataDeleter} empties itself. Add to this only along with the code that does the removing.
	 */
	private static final Set<String> REMOVED_BY_THE_DELETER = Set.of();

	@Test
	void everythingPointingAtAnErrandIsTakenWithIt() throws IOException {
		final var covered = Stream.concat(mappedOnTheErrand().stream(), REMOVED_BY_THE_DELETER.stream()).collect(toSet());

		final var uncovered = tablesReferencingErrandWithoutDatabaseCascade().stream()
			.filter(table -> !covered.contains(table))
			.sorted()
			.toList();

		assertThat(uncovered)
			.as("""
				These tables point at errand without the database cascading, without being mapped on ErrandEntity and \
				without being emptied by ErrandDataDeleter. An errand carrying a row in one of them can be removed \
				neither by a delete nor by a purge. Give the relation a mapping on ErrandEntity, remove the rows in \
				ErrandDataDeleter and name the table in REMOVED_BY_THE_DELETER, or let the constraint cascade.""")
			.isEmpty();
	}

	/**
	 * The tables whose rows the database would leave behind, and that something on this side therefore has to remove.
	 */
	private static Set<String> tablesReferencingErrandWithoutDatabaseCascade() throws IOException {
		final var schema = readSchema().replaceAll("\\s+", " ");
		final var matcher = FOREIGN_KEY_TO_ERRAND.matcher(schema);
		final var tables = new HashSet<String>();

		while (matcher.find()) {
			if (matcher.group(2) == null) {
				tables.add(matcher.group(1));
			}
		}

		// A schema carrying no such constraint at all would pass this test without meaning to
		assertThat(tables).as("No foreign key to errand was found in %s, so this test is reading the wrong thing".formatted(SCHEMA)).isNotEmpty();

		return tables;
	}

	/**
	 * The tables JPA takes with the errand, read from the collections the entity holds.
	 */
	private static Set<String> mappedOnTheErrand() {
		return Stream.of(ErrandEntity.class.getDeclaredFields())
			.map(ErrandRemovalCoverageTest::tableOf)
			.flatMap(Optional::stream)
			.collect(toSet());
	}

	private static Optional<String> tableOf(final Field field) {
		if (field.isAnnotationPresent(ElementCollection.class)) {
			return Optional.ofNullable(field.getAnnotation(CollectionTable.class)).map(CollectionTable::name);
		}
		if (field.isAnnotationPresent(OneToMany.class)) {
			return tableOfTarget(field);
		}
		return Optional.empty();
	}

	private static Optional<String> tableOfTarget(final Field field) {
		if (field.getGenericType() instanceof final ParameterizedType parameterized
			&& parameterized.getActualTypeArguments()[0] instanceof final Class<?> target) {

			return Optional.ofNullable(target.getAnnotation(Table.class)).map(Table::name);
		}
		return Optional.empty();
	}

	private static String readSchema() throws IOException {
		try (var stream = Objects.requireNonNull(ErrandRemovalCoverageTest.class.getClassLoader().getResourceAsStream(SCHEMA), SCHEMA + " is missing")) {
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Verifies that every collection an errand holds resolves to a table name.
	 */
	@Test
	void theCollectionsOfAnErrandResolveToTables() {
		final var collections = Stream.of(ErrandEntity.class.getDeclaredFields())
			.filter(field -> field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ElementCollection.class))
			.toList();

		assertThat(collections).isNotEmpty();
		assertThat(collections.stream().map(ErrandRemovalCoverageTest::tableOf).flatMap(Optional::stream).toList())
			.as("Every collection an errand holds has to resolve to a table name for the coverage test to mean anything")
			.hasSameSizeAs(collections)
			.doesNotContain("");
	}
}
