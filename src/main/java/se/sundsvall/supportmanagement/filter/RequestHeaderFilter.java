package se.sundsvall.supportmanagement.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.NOTIFY_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearNotify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setNotify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setRequestGroupId;

/**
 * Keeps the {@value se.sundsvall.supportmanagement.service.util.ServiceUtil#REQUEST_GROUP_ID_HEADER} and
 * {@value se.sundsvall.supportmanagement.service.util.ServiceUtil#NOTIFY_HEADER} headers on the thread for the length
 * of the request, so that whatever the request does can read them without the values being passed along through every
 * layer.
 */
@Component
public class RequestHeaderFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
		throws ServletException, IOException {

		// A request that names no group is a group of its own, so that the events it logs still reach the subscribers as
		// one notification
		setRequestGroupId(ofNullable(request.getHeader(REQUEST_GROUP_ID_HEADER))
			.filter(StringUtils::isNotBlank)
			.orElseGet(() -> randomUUID().toString()));
		setNotify(request.getHeader(NOTIFY_HEADER));
		try {
			filterChain.doFilter(request, response);
			response.setHeader(REQUEST_GROUP_ID_HEADER, getRequestGroupId());
		} finally {
			clearRequestGroupId();
			clearNotify();
		}
	}
}
