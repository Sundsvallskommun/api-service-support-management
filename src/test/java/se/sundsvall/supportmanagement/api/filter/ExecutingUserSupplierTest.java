package se.sundsvall.supportmanagement.api.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SpringBootTest(classes = ExecutingUserSupplier.class, webEnvironment = WebEnvironment.MOCK)
class ExecutingUserSupplierTest {
	private static final String AD_USER_HEADER_KEY = "sentbyuser";
	private static final String UNKNOWN = "UNKNOWN";

	@MockBean
	private HttpServletRequest requestMock;

	@MockBean
	private HttpServletResponse responseMock;

	@MockBean
	private FilterChain filterChainMock;

	@Autowired
	private ExecutingUserSupplier executingUserSupplier;

	@Test
	void verifyAutowiring() {
		assertThat(executingUserSupplier).isNotNull();
	}

	@Test
	void doFilterInternal() throws Exception {
		executingUserSupplier.doFilterInternal(requestMock, responseMock, filterChainMock);

		verify(requestMock).getHeader(AD_USER_HEADER_KEY);
		verify(filterChainMock).doFilter(requestMock, responseMock);
	}

	@Test
	void extractUserWhenPresent() throws Exception {
		final var adUser = "adUser";

		when(requestMock.getHeader(AD_USER_HEADER_KEY)).thenReturn(adUser);

		executingUserSupplier.extractAdUser(requestMock);

		verify(requestMock).getHeader(AD_USER_HEADER_KEY);
		assertThat(executingUserSupplier.getAdUser()).isEqualTo(adUser);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void extractUserWhenNotPresent(String adUser) throws Exception {
		when(requestMock.getHeader(AD_USER_HEADER_KEY)).thenReturn(adUser);

		executingUserSupplier.extractAdUser(requestMock);

		verify(requestMock).getHeader(AD_USER_HEADER_KEY);
		assertThat(executingUserSupplier.getAdUser()).isEqualTo(UNKNOWN);
	}
}
