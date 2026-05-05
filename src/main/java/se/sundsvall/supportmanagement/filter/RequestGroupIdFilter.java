package se.sundsvall.supportmanagement.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;

@Component
public class RequestGroupIdFilter extends OncePerRequestFilter {

	static final String REQUEST_GROUP_ID_ATTRIBUTE = "requestGroupId";

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
		throws ServletException, IOException {

		final var incomingId = request.getHeader(REQUEST_GROUP_ID_HEADER);
		final var requestGroupId = StringUtils.isNotBlank(incomingId) ? incomingId : UUID.randomUUID().toString();

		request.setAttribute(REQUEST_GROUP_ID_ATTRIBUTE, requestGroupId);
		filterChain.doFilter(request, response);
		response.setHeader(REQUEST_GROUP_ID_HEADER, (String) request.getAttribute(REQUEST_GROUP_ID_ATTRIBUTE));
	}

	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) {
		return !request.getRequestURI().contains("/errands");
	}
}
