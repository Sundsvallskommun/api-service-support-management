package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class ProtectedResourceTest {

	/**
	 * The path is what an access pattern is matched against, and what the API reports in place of the constant. Two
	 * resources sharing one would be granted together and reported as each other.
	 */
	@Test
	void everyResourceCarriesADistinctPath() {
		assertThat(Arrays.stream(ProtectedResource.values()).map(ProtectedResource::getPath).collect(toSet()))
			.hasSize(ProtectedResource.values().length);
	}

	@Test
	void everyPathIsLowerCaseAndCarriesNoSurroundingSeparator() {
		assertThat(Arrays.stream(ProtectedResource.values()).map(ProtectedResource::getPath))
			.allSatisfy(path -> assertThat(path)
				.isEqualTo(path.toLowerCase(java.util.Locale.ROOT))
				.doesNotStartWith("/")
				.doesNotEndWith("/")
				.isNotBlank());
	}

	@Test
	void onlyTheResourcesBelongingToAnErrandAreErrandScoped() {
		assertThat(Arrays.stream(ProtectedResource.values()).filter(ProtectedResource::isErrandScoped))
			.allSatisfy(resource -> assertThat(resource.getPath()).startsWith("errand"))
			.contains(ProtectedResource.ERRAND, ProtectedResource.PARAMETER, ProtectedResource.CONVERSATION_ATTACHMENT);

		assertThat(Arrays.stream(ProtectedResource.values()).filter(resource -> !resource.isErrandScoped()))
			.allSatisfy(resource -> assertThat(resource.getPath()).doesNotStartWith("errand"))
			.contains(ProtectedResource.NAMESPACE_CONFIG, ProtectedResource.METADATA_LABEL);
	}
}
