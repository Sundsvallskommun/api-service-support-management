package se.sundsvall.supportmanagement.api.util;

import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.api.util.JsonParameterUtil.toUpsertResponse;
import static se.sundsvall.supportmanagement.api.util.JsonParameterUtil.verifyKeyMatchesPath;

class JsonParameterUtilTest {

	private static final String KEY = "key";
	private static final String PATH = "/2281/NS/errands/e1/statements/s1/json-parameters/key";

	@BeforeEach
	void setCurrentRequest() {
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("PUT", PATH)));
	}

	@AfterEach
	void resetCurrentRequest() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void verifyKeyMatchesPathWithSameKey() {

		// Act & Verify
		assertThatNoException().isThrownBy(() -> verifyKeyMatchesPath(JsonParameter.create().withKey(KEY), KEY));
	}

	/**
	 * A body that omits the key says nothing about it, and the path decides.
	 */
	@Test
	void verifyKeyMatchesPathWithoutKeyInBody() {

		// Act & Verify
		assertThatNoException().isThrownBy(() -> verifyKeyMatchesPath(JsonParameter.create(), KEY));
	}

	@Test
	void verifyKeyMatchesPathWithAnotherKeyInBody() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> verifyKeyMatchesPath(JsonParameter.create().withKey("otherKey"), KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getDetail()).isEqualTo("Key in request body 'otherKey' does not match key in path 'key'");
	}

	@Test
	void toUpsertResponseForCreatedParameter() {

		// Arrange
		final var jsonParameter = JsonParameter.create().withKey(KEY).withVersion(0L);

		// Act
		final var response = toUpsertResponse(new UpsertResult(jsonParameter, true));

		// Verify
		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create(PATH));
		assertThat(response.getHeaders().getETag()).isEqualTo("\"0\"");
		assertThat(response.getBody()).isSameAs(jsonParameter);
	}

	@Test
	void toUpsertResponseForReplacedParameter() {

		// Arrange
		final var jsonParameter = JsonParameter.create().withKey(KEY).withVersion(3L);

		// Act
		final var response = toUpsertResponse(new UpsertResult(jsonParameter, false));

		// Verify
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getHeaders().getLocation()).isNull();
		assertThat(response.getHeaders().getETag()).isEqualTo("\"3\"");
		assertThat(response.getBody()).isSameAs(jsonParameter);
	}

	/**
	 * A parameter without a version is answered without an ETag value rather than with an empty one.
	 */
	@Test
	void toUpsertResponseWithoutVersion() {

		// Arrange
		final var jsonParameter = JsonParameter.create().withKey(KEY);

		// Act
		final var response = toUpsertResponse(new UpsertResult(jsonParameter, false));

		// Verify
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getHeaders().getETag()).isNull();
	}
}
