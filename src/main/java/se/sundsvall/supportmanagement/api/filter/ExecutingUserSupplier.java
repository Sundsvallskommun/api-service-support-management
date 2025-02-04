package se.sundsvall.supportmanagement.api.filter;

import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.Constants.AD_USER_HEADER_KEY;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ExecutingUserSupplier extends OncePerRequestFilter {

	public static final String UNKNOWN = "UNKNOWN";

	private static final ThreadLocal<String> THREAD_LOCAL_AD_USER = new ThreadLocal<>();

	public String getAdUser() {
		return THREAD_LOCAL_AD_USER.get();
	}

	@Override
	protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		// Extract AD-user from ad-user header
		THREAD_LOCAL_AD_USER.set(extractAdUser(request));

		try {
			filterChain.doFilter(request, response);
		} finally {
			// Remove value from threadlocal when filter chain has been processed
			THREAD_LOCAL_AD_USER.remove();
		}
	}

	String extractAdUser(HttpServletRequest request) {
		return ofNullable(request.getHeader(AD_USER_HEADER_KEY))
			.filter(StringUtils::hasText)
			.orElse(UNKNOWN);
	}
}
