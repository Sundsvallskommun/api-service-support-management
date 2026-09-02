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
 * Reminds developers that a table pointing at an errand has to be taken with it when the errand goes.
 * <p>
 * An errand is removed in two places - the single delete and the retention purge - and both end by deleting the errand
 * row. Whatever still points at it then stops the removal: the row cannot go, the transaction fails, and the errand
 * stays. For a purge that shows up only as a number in the job it reports against, one errand at a time, in production.
 * <p>
 * Three things make a table safe. The database can cascade it, {@link ErrandEntity} can map it so that JPA cascades it,
 * or {@link ErrandDataDeleter} can remove the rows itself. A new table needs one of the three, and a unidirectional
 * many to one gets none of them by default - which is why this test exists rather than the rule being left to memory.
 * <p>
 * This is an early warning, not a proof. It reads the schema the entities generate, so a table added by a migration
 * alone is invisible to it.
 */
class ErrandRemovalCoverageTest {

	private static final String SCHEMA = "db/scripts/schema.sql";

	/**
	 * Constraints on the errand table, and whether the database takes the rows with it. Read from the schema with the
	 * whitespace flattened, since the statements are written across several lines.
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
	 * Guards the reading rather than the code: a mapping the test cannot resolve to a table would quietly widen what
	 * counts as covered.
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
