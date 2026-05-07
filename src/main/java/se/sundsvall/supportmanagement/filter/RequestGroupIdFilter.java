package se.sundsvall.supportmanagement.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setRequestGroupId;

@Component
public class RequestGroupIdFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
		throws ServletException, IOException {

		setRequestGroupId(request.getHeader(REQUEST_GROUP_ID_HEADER));
		try {
			filterChain.doFilter(request, response);
			response.setHeader(REQUEST_GROUP_ID_HEADER, getRequestGroupId());
		} finally {
			clearRequestGroupId();
		}
	}
}
