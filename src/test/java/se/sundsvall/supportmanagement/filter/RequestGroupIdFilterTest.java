package se.sundsvall.supportmanagement.filter;

import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;

@ExtendWith(MockitoExtension.class)
class RequestGroupIdFilterTest {

	@Mock
	private FilterChain filterChainMock;

	@InjectMocks
	private RequestGroupIdFilter filter;

	@Test
	void setsNullAndNoResponseHeaderWhenHeaderMissing() throws Exception {
		final var request = new MockHttpServletRequest();
		final var response = new MockHttpServletResponse();

		filter.doFilterInternal(request, response, filterChainMock);

		assertThat(response.getHeader(REQUEST_GROUP_ID_HEADER)).isNull();
		verify(filterChainMock).doFilter(request, response);
	}

	@Test
	void usesIncomingHeaderValueWhenPresent() throws Exception {
		final var existingGroupId = UUID.randomUUID().toString();
		final var request = new MockHttpServletRequest();
		request.addHeader(REQUEST_GROUP_ID_HEADER, existingGroupId);
		final var response = new MockHttpServletResponse();

		filter.doFilterInternal(request, response, filterChainMock);

		assertThat(response.getHeader(REQUEST_GROUP_ID_HEADER)).isEqualTo(existingGroupId);
	}

}
