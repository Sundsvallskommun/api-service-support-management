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
import static se.sundsvall.supportmanagement.filter.RequestGroupIdFilter.REQUEST_GROUP_ID_ATTRIBUTE;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;

@ExtendWith(MockitoExtension.class)
class RequestGroupIdFilterTest {

	@Mock
	private FilterChain filterChainMock;

	@InjectMocks
	private RequestGroupIdFilter filter;

	@Test
	void setsRequestGroupIdAttributeAndResponseHeader() throws Exception {
		final var request = new MockHttpServletRequest();
		final var response = new MockHttpServletResponse();

		filter.doFilterInternal(request, response, filterChainMock);

		final var attribute = (String) request.getAttribute(REQUEST_GROUP_ID_ATTRIBUTE);
		assertThat(attribute).isNotBlank();
		assertThat(UUID.fromString(attribute)).isNotNull();
		assertThat(response.getHeader(REQUEST_GROUP_ID_HEADER)).isEqualTo(attribute);
		verify(filterChainMock).doFilter(request, response);
	}

	@Test
	void eachRequestGetsUniqueRequestGroupId() throws Exception {
		final var request1 = new MockHttpServletRequest();
		final var request2 = new MockHttpServletRequest();

		filter.doFilterInternal(request1, new MockHttpServletResponse(), filterChainMock);
		filter.doFilterInternal(request2, new MockHttpServletResponse(), filterChainMock);

		assertThat(request1.getAttribute(REQUEST_GROUP_ID_ATTRIBUTE))
			.isNotEqualTo(request2.getAttribute(REQUEST_GROUP_ID_ATTRIBUTE));
	}

	@Test
	void usesIncomingHeaderValueWhenPresent() throws Exception {
		final var existingGroupId = UUID.randomUUID().toString();
		final var request = new MockHttpServletRequest();
		request.addHeader(REQUEST_GROUP_ID_HEADER, existingGroupId);
		final var response = new MockHttpServletResponse();

		filter.doFilterInternal(request, response, filterChainMock);

		assertThat(request.getAttribute(REQUEST_GROUP_ID_ATTRIBUTE)).isEqualTo(existingGroupId);
		assertThat(response.getHeader(REQUEST_GROUP_ID_HEADER)).isEqualTo(existingGroupId);
	}

	@Test
	void shouldNotFilterNonErrandPaths() {
		final var request = new MockHttpServletRequest();
		request.setRequestURI("/2281/MY_NAMESPACE/notifications");

		assertThat(filter.shouldNotFilter(request)).isTrue();
	}

	@Test
	void shouldFilterErrandPaths() {
		final var request = new MockHttpServletRequest();
		request.setRequestURI("/2281/MY_NAMESPACE/errands/b82bd8ac-1507-4d9a-958d-369261eecc15");

		assertThat(filter.shouldNotFilter(request)).isFalse();
	}
}
