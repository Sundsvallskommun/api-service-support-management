package se.sundsvall.supportmanagement;

import java.util.Map;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Starts the OpenSearch container the integration tests index into and hands its address to Hibernate Search as
 * {@code opensearch.hosts}, which application-it.yml points the backend at.
 * <p>
 * Registered through META-INF/spring.factories, the same way the MariaDB container is reached through the
 * {@code jdbc:tc:} url, so that no test class has to know about it. It runs for every application context on the test
 * classpath, so it only acts when the profile in use has Hibernate Search switched on, which the unit test profile has
 * not. The container is reusable and shared by every context of the run.
 */
public class OpenSearchContainerEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String HOSTS_PROPERTY = "opensearch.hosts";

	// Same version as the chart deploys and the highest one Hibernate Search 8.4 lists as tested
	private static final String IMAGE = "opensearchproject/opensearch:3.6.0";
	private static final String SEARCH_ENABLED_PROPERTY = "spring.jpa.properties.hibernate.search.enabled";

	private static final OpenSearchContainer<?> CONTAINER = new OpenSearchContainer<>(IMAGE)
		.withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
		.withReuse(true);

	@Override
	public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
		if (!environment.getProperty(SEARCH_ENABLED_PROPERTY, Boolean.class, false)) {
			return;
		}

		CONTAINER.start();
		// Hibernate Search wants host:port, the container answers with the scheme in front
		final var hosts = CONTAINER.getHttpHostAddress().replaceFirst("^https?://", "");
		environment.getPropertySources().addFirst(new MapPropertySource("opensearchContainer", Map.of(HOSTS_PROPERTY, hosts)));
	}
}
