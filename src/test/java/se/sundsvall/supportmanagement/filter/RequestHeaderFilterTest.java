package se.sundsvall.supportmanagement.filter;

import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.NOTIFY_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearNotify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.shouldNotify;

@ExtendWith(MockitoExtension.class)
class RequestHeaderFilterTest {

	@Mock
	private FilterChain filterChainMock;

	@InjectMocks
	private RequestHeaderFilter filter;

	@AfterEach
	void clear() {
		clearRequestGroupId();
		clearNotify();
	}

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

	@Test
	void notifiesWhenHeaderMissing() throws Exception {
		final var request = new MockHttpServletRequest();
		final var response = new MockHttpServletResponse();
		final var notifyDuringRequest = new boolean[1];
		doAnswer(_ -> notifyDuringRequest[0] = shouldNotify()).when(filterChainMock).doFilter(any(), any());

		filter.doFilterInternal(request, response, filterChainMock);

		assertThat(notifyDuringRequest[0]).isTrue();
		verify(filterChainMock).doFilter(request, response);
	}

	@Test
	void doesNotNotifyDuringRequestWhenHeaderIsFalse() throws Exception {
		final var request = new MockHttpServletRequest();
		request.addHeader(NOTIFY_HEADER, "false");
		final var response = new MockHttpServletResponse();
		final var notifyDuringRequest = new boolean[] {
			true
		};
		doAnswer(_ -> notifyDuringRequest[0] = shouldNotify()).when(filterChainMock).doFilter(any(), any());

		filter.doFilterInternal(request, response, filterChainMock);

		assertThat(notifyDuringRequest[0]).isFalse();
		assertThat(shouldNotify()).isTrue();
	}

	@Test
	void clearsValuesWhenChainFails() throws Exception {
		final var request = new MockHttpServletRequest();
		request.addHeader(REQUEST_GROUP_ID_HEADER, UUID.randomUUID().toString());
		request.addHeader(NOTIFY_HEADER, "false");
		final var response = new MockHttpServletResponse();
		doThrow(new IllegalStateException("boom")).when(filterChainMock).doFilter(any(), any());

		assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChainMock)).isInstanceOf(IllegalStateException.class);

		assertThat(getRequestGroupId()).isNull();
		assertThat(shouldNotify()).isTrue();
	}
}
