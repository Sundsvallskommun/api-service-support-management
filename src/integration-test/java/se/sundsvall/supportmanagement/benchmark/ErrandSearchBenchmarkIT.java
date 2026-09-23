package se.sundsvall.supportmanagement.benchmark;

import generated.se.sundsvall.accessmapper.Access;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.AccessMapperService;
import se.sundsvall.supportmanagement.service.MetadataService;
import se.sundsvall.supportmanagement.service.access.AccessSnapshot;
import se.sundsvall.supportmanagement.service.search.ErrandSearchService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * What an errand search costs, and what access control adds to it.
 * <p>
 * Seeds a namespace of its own with a corpus large enough for the numbers to mean something, indexes it, and times the
 * same queries for users reaching the corpus in different ways: the cheapest route the index can take (no access
 * control at all), one where every label of the namespace is held, ones where the excluded labels have to be listed,
 * and one where the errands are reached by two routes at once. Not a test: nothing is asserted, the numbers are
 * printed.
 * <p>
 * Off unless asked for, since seeding and indexing a hundred thousand errands takes minutes:
 *
 * <pre>
 * mvn process-test-resources failsafe:integration-test -Dit.test=ErrandSearchBenchmarkIT -Dsearch.benchmark=true
 * </pre>
 *
 * The corpus size and the number of labels can be moved with {@code -Dsearch.benchmark.errands} and
 * {@code -Dsearch.benchmark.labels}.
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
	// The integration test profile turns caching off; this benchmark is partly about what a cache is worth
	"spring.cache.type=caffeine"
})
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "search.benchmark", matches = "true")
class ErrandSearchBenchmarkIT {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-BENCH";
	private static final long CONFIG_ID = 9000;

	private static final int ERRANDS = Integer.getInteger("search.benchmark.errands", 100_000);
	private static final int LABELS = Integer.getInteger("search.benchmark.labels", 120);
	private static final int WARMUP = 5;
	private static final int RUNS = 30;

	/** In the title of every thousandth errand, so that a free text search over them finds a hundred of a hundred thousand. */
	private static final String RARE = "kvarnbacken";
	/** In the description of every other errand. */
	private static final String COMMON = "gatubelysning";
	/** In nothing at all, so that the query costs what the index and the access filter cost and nothing else. */
	private static final String ABSENT = "title:zzzzzzzz";

	private static final List<String> STATUSES = List.of("NEW", "ONGOING", "SOLVED", "SUSPENDED");

	@Autowired
	private DataSource dataSource;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ErrandSearchService searchService;

	@Autowired
	private CacheManager cacheManager;

	@MockitoBean
	private AccessMapperService accessMapperServiceMock;

	private JdbcTemplate jdbc;
	private List<MetadataLabelEntity> labels;
	private final List<String> report = new ArrayList<>();

	@BeforeAll
	void seedAndIndex() {
		jdbc = new JdbcTemplate(dataSource);
		labels = IntStream.range(0, LABELS)
			.mapToObj(index -> MetadataLabelEntity.create().withId("bench-label-%03d".formatted(index)))
			.toList();

		final var seeded = time(this::seed);
		final var indexed = time(() -> {
			try {
				Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer()
					.threadsToLoadObjects(4)
					.batchSizeToLoadObjects(1000)
					.startAndWait();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
		});

		line("");
		line("# Errand search benchmark");
		line("");
		line("%d errands, %d labels, one label per errand. Each errand carries a stakeholder with a contact channel, a parameter, a JSON parameter".formatted(ERRANDS, LABELS));
		line("and a communication. Seeded in %d s, indexed in %d s.".formatted(seeded / 1000, indexed / 1000));
	}

	@Test
	void benchmark() {
		// The cheapest the index can answer: no access control, so no label filter at all
		accessControl(false);
		run("access control off", null);

		accessControl(true);
		run("every label held (%d of %d, nothing to exclude)".formatted(LABELS, LABELS), snapshot(labels, List.of()));
		run("half the labels held (%d of %d, %d excluded)".formatted(LABELS / 2, LABELS, LABELS - LABELS / 2), snapshot(labels.subList(0, LABELS / 2), List.of()));
		run("one label held (1 of %d, %d excluded)".formatted(LABELS, LABELS - 1), snapshot(labels.subList(0, 1), List.of()));
		run("one at read, half at limited read (two clauses)", snapshot(labels.subList(0, 1), labels.subList(0, LABELS / 2)));

		// The same as the one label profile, with the label id cache thrown away before every call
		run("one label held, label cache cold", snapshot(labels.subList(0, 1), List.of()), true);

		report.forEach(System.out::println);
	}

	private void run(final String profile, final AccessSnapshot snapshot) {
		run(profile, snapshot, false);
	}

	private void run(final String profile, final AccessSnapshot snapshot, final boolean coldCache) {
		when(accessMapperServiceMock.getAccessSnapshot(anyString(), anyString(), any())).thenReturn(snapshot == null ? AccessSnapshot.empty() : snapshot);
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("ben01mrk"));

		line("");
		line("## %s".formatted(profile));
		line("");
		line("| query | page | hits | p50 | p95 | max |");
		line("|---|---:|---:|---:|---:|---:|");

		final var newestFirst = Sort.by(Sort.Direction.DESC, "created");

		measure("no hits at all (index only)", ABSENT, Sort.unsorted(), 20, coldCache);
		measure("(blank, newest first)", "", newestFirst, 1, coldCache);
		measure("(blank, newest first)", "", newestFirst, 20, coldCache);
		measure("(blank, newest first)", "", newestFirst, 100, coldCache);
		measure("free text, rare word", RARE, Sort.unsorted(), 20, coldCache);
		measure("free text, common word", COMMON, Sort.unsorted(), 20, coldCache);
		measure("status:ONGOING", "status:ONGOING", Sort.unsorted(), 20, coldCache);
		measure("title:%s".formatted(RARE), "title:" + RARE, Sort.unsorted(), 20, coldCache);
	}

	private void measure(final String label, final String query, final Sort sort, final int size, final boolean coldCache) {
		final var pageable = PageRequest.of(0, size, sort);
		long hits = 0;

		for (var i = 0; i < WARMUP; i++) {
			hits = searchService.search(NAMESPACE, MUNICIPALITY_ID, query, pageable).getTotalElements();
		}

		final var timings = new ArrayList<Long>(RUNS);
		for (var i = 0; i < RUNS; i++) {
			if (coldCache) {
				cacheManager.getCache(MetadataService.LABEL_IDS_CACHE_NAME).clear();
			}
			final var started = System.nanoTime();
			searchService.search(NAMESPACE, MUNICIPALITY_ID, query, pageable);
			timings.add((System.nanoTime() - started) / 1_000_000);
		}

		timings.sort(null);
		line("| %s | %d | %d | %d ms | %d ms | %d ms |".formatted(label, size, hits,
			timings.get(RUNS / 2), timings.get((int) (RUNS * 0.95)), timings.getLast()));
	}

	/**
	 * What the access mapper says about a user reaching the corpus by sent in labels: the first set at read, the second
	 * at limited read.
	 */
	private static AccessSnapshot snapshot(final List<MetadataLabelEntity> read, final List<MetadataLabelEntity> limited) {
		final var byLevel = new java.util.EnumMap<Access.AccessLevelEnum, Set<MetadataLabelEntity>>(Access.AccessLevelEnum.class);
		byLevel.put(Access.AccessLevelEnum.R, Set.copyOf(read));
		if (!limited.isEmpty()) {
			byLevel.put(Access.AccessLevelEnum.LR, Set.copyOf(limited));
		}
		return new AccessSnapshot(byLevel, Set.of(), Map.of());
	}

	private void accessControl(final boolean enforced) {
		jdbc.update("update namespace_config_value set `value` = ? where namespace_config_id = ? and `key` = 'ACCESS_CONTROL'", String.valueOf(enforced), CONFIG_ID);
		cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
	}

	private void seed() {
		jdbc.update("delete from contact_channel where stakeholder_id in (select id from stakeholder where errand_id in (select id from errand where namespace = ?))", NAMESPACE);
		jdbc.update("delete from stakeholder where errand_id in (select id from errand where namespace = ?)", NAMESPACE);
		jdbc.update("delete from parameter_values where parameter_id in (select id from parameter where errand_id in (select id from errand where namespace = ?))", NAMESPACE);
		jdbc.update("delete from parameter where errand_id in (select id from errand where namespace = ?)", NAMESPACE);
		jdbc.update("delete from json_parameter where errand_id in (select id from errand where namespace = ?)", NAMESPACE);
		jdbc.update("delete from communication where namespace = ?", NAMESPACE);
		jdbc.update("delete from errand_access_labels where errand_id in (select id from errand where namespace = ?)", NAMESPACE);
		jdbc.update("delete from errand where namespace = ?", NAMESPACE);
		jdbc.update("delete from metadata_label where namespace = ?", NAMESPACE);
		jdbc.update("delete from namespace_config_value where namespace_config_id = ?", CONFIG_ID);
		jdbc.update("delete from namespace_config where id = ?", CONFIG_ID);

		jdbc.update("insert into namespace_config(id, municipality_id, namespace, created) values (?, ?, ?, now())", CONFIG_ID, MUNICIPALITY_ID, NAMESPACE);
		jdbc.batchUpdate("insert into namespace_config_value(namespace_config_id, `key`, `value`, `type`) values (?, ?, ?, ?)", List.of(
			new Object[] {
				CONFIG_ID, "DISPLAY_NAME", "Benchmark", "STRING"
			},
			new Object[] {
				CONFIG_ID, "SHORT_CODE", "BM", "STRING"
			},
			new Object[] {
				CONFIG_ID, "NOTIFICATION_TTL_IN_DAYS", "40", "INTEGER"
			},
			new Object[] {
				CONFIG_ID, "ACCESS_CONTROL", "true", "BOOLEAN"
			},
			new Object[] {
				CONFIG_ID, "NOTIFY_REPORTER", "false", "BOOLEAN"
			},
			new Object[] {
				CONFIG_ID, "ROLE_BASED_MAPPING", "false", "BOOLEAN"
			},
			new Object[] {
				CONFIG_ID, "RESOURCE_ACCESS_CONTROL", "false", "BOOLEAN"
			}));

		jdbc.batchUpdate("insert into metadata_label(created, municipality_id, namespace, classification, display_name, id, resource_name, resource_path, deprecated) "
			+ "values (now(), ?, ?, 'CLASS', ?, ?, ?, ?, false)",
			labels.stream().map(label -> new Object[] {
				MUNICIPALITY_ID, NAMESPACE, label.getId(), label.getId(), label.getId(), label.getId()
			}).toList());

		final var errands = new ArrayList<Object[]>(1000);
		final var errandLabels = new ArrayList<Object[]>(1000);
		final var stakeholders = new ArrayList<Object[]>(1000);
		final var contactChannels = new ArrayList<Object[]>(1000);
		final var parameters = new ArrayList<Object[]>(1000);
		final var parameterValues = new ArrayList<Object[]>(1000);
		final var jsonParameters = new ArrayList<Object[]>(1000);
		final var communications = new ArrayList<Object[]>(1000);

		for (var index = 0; index < ERRANDS; index++) {
			final var id = "bb000000-0000-0000-0000-%012d".formatted(index);
			final var title = index % 1000 == 0
				? "Anmälan om %s vid hållplatsen".formatted(RARE)
				: "Anmälan nummer %d om trasig utrustning".formatted(index);
			final var description = index % 2 == 0
				? "Det är fel på %s sedan en tid tillbaka och ingen har hört av sig".formatted(COMMON)
				: "Felanmälan %d gäller en trasig detalj som behöver bytas".formatted(index);

			errands.add(new Object[] {
				MUNICIPALITY_ID, id, NAMESPACE, "MEDIUM", STATUSES.get(index % STATUSES.size()), "GATA", "BELYSNING",
				title, description, "rep01ort", "BM-%08d".formatted(index)
			});
			errandLabels.add(new Object[] {
				id, labels.get(index % LABELS).getId()
			});

			// What hangs off an errand, so that building a page of them costs what it costs in the real thing
			// The stakeholder key is numeric, unlike the other keys of the model
			final var stakeholderId = 1_000_000L + index;
			final var parameterId = "pp000000-0000-0000-0000-%012d".formatted(index);
			stakeholders.add(new Object[] {
				stakeholderId, "aa000000-0000-0000-0000-%012d".formatted(index), "PRIVATE", id, "Anna", "Bergström %d".formatted(index), "APPLICANT", "Storgatan %d".formatted(index % 200), "85230", "Sundsvall"
			});
			contactChannels.add(new Object[] {
				stakeholderId, "EMAIL", "anna.%d@example.com".formatted(index)
			});
			parameters.add(new Object[] {
				id, parameterId, "location", "Plats", "Ärende"
			});
			parameterValues.add(new Object[] {
				parameterId, "Storgatan %d".formatted(index % 200), 0
			});
			jsonParameters.add(new Object[] {
				"jp000000-0000-0000-0000-%012d".formatted(index), id, "vehicle", "vehicle-1.0",
				"{\"regNo\":\"ABC%05d\",\"owner\":{\"name\":\"Anna Bergström\"},\"tags\":[\"tjänstebil\"]}".formatted(index % 100000)
			});
			communications.add(new Object[] {
				"cc000000-0000-0000-0000-%012d".formatted(index), "BM-%08d".formatted(index),
				"Fortfarande fel på utrustningen, kan ni titta på det", "Uppföljning av ärende %d".formatted(index), NAMESPACE, MUNICIPALITY_ID
			});

			if (errands.size() == 1000 || index == ERRANDS - 1) {
				flush(errands, errandLabels, stakeholders, contactChannels, parameters, parameterValues, jsonParameters, communications);
			}
		}
	}

	private void flush(final List<Object[]> errands, final List<Object[]> errandLabels, final List<Object[]> stakeholders, final List<Object[]> contactChannels,
		final List<Object[]> parameters, final List<Object[]> parameterValues, final List<Object[]> jsonParameters, final List<Object[]> communications) {

		jdbc.batchUpdate("insert into errand(municipality_id, id, namespace, priority, status, category, type, title, description, reporter_user_id, "
			+ "created, touched, errand_number, business_related) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), ?, false)", errands);
		jdbc.batchUpdate("insert into errand_access_labels(errand_id, metadata_label_id) values (?, ?)", errandLabels);
		jdbc.batchUpdate("insert into stakeholder(id, external_id, external_id_type, errand_id, first_name, last_name, role, address, zip_code, city) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", stakeholders);
		jdbc.batchUpdate("insert into contact_channel(stakeholder_id, type, value) values (?, ?, ?)", contactChannels);
		jdbc.batchUpdate("insert into parameter(errand_id, id, parameters_key, display_name, parameter_group) values (?, ?, ?, ?, ?)", parameters);
		jdbc.batchUpdate("insert into parameter_values(parameter_id, value, value_order) values (?, ?, ?)", parameterValues);
		jdbc.batchUpdate("insert into json_parameter(id, errand_id, parameter_key, schema_id, value, version) values (?, ?, ?, ?, ?, 0)", jsonParameters);
		jdbc.batchUpdate("insert into communication(internal, viewed, sender, sender_user_id, sent, id, errand_number, external_id, message_body, target, "
			+ "subject, direction, type, namespace, municipality_id, html_message_body) "
			+ "values (0, 0, 'Anna Bergström', null, now(), ?, ?, null, ?, 'anna@example.com', ?, 'INBOUND', 'EMAIL', ?, ?, null)", communications);

		errands.clear();
		errandLabels.clear();
		stakeholders.clear();
		contactChannels.clear();
		parameters.clear();
		parameterValues.clear();
		jsonParameters.clear();
		communications.clear();
	}

	private void line(final String text) {
		report.add(text);
	}

	private static long time(final Runnable work) {
		final var started = System.currentTimeMillis();
		work.run();
		return System.currentTimeMillis() - started;
	}
}
