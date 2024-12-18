package se.sundsvall.supportmanagement.api.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = ExecutingUserSupplier.class, webEnvironment = WebEnvironment.MOCK)
class ExecutingUserSupplierTest {
	private static final String AD_USER_HEADER_KEY = "sentbyuser";
	private static final String UNKNOWN = "UNKNOWN";

	@MockitoBean
	private HttpServletRequest requestMock;

	@MockitoBean
	private HttpServletResponse responseMock;

	@MockitoBean
	private FilterChain filterChainMock;

	@Autowired
	private ExecutingUserSupplier executingUserSupplier;

	@Test
	void verifyAutowiring() {
		assertThat(executingUserSupplier).isNotNull();
	}

	@Test
	void doFilterInternal() throws Exception {
		final var adUser = "adUser";

		when(requestMock.getHeader(AD_USER_HEADER_KEY)).thenReturn(adUser);

		doAnswer((Answer<Object>) invocation -> {
			assertThat(executingUserSupplier.getAdUser()).isEqualTo(adUser);
			return null;
		}).when(filterChainMock).doFilter(requestMock, responseMock);

		executingUserSupplier.doFilterInternal(requestMock, responseMock, filterChainMock);

		assertThat(executingUserSupplier.getAdUser()).isNull();
		verify(requestMock).getHeader(AD_USER_HEADER_KEY);
		verify(filterChainMock).doFilter(requestMock, responseMock);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void doFilterInternalWhenNoUserPresent(final String adUser) throws Exception {

		doAnswer((Answer<Object>) invocation -> {
			assertThat(executingUserSupplier.getAdUser()).isEqualTo(UNKNOWN);
			return null;
		}).when(filterChainMock).doFilter(requestMock, responseMock);

		executingUserSupplier.doFilterInternal(requestMock, responseMock, filterChainMock);

		assertThat(executingUserSupplier.getAdUser()).isNull();
		verify(requestMock).getHeader(AD_USER_HEADER_KEY);
		verify(filterChainMock).doFilter(requestMock, responseMock);
	}
}
