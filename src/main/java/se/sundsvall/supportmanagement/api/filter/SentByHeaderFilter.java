package se.sundsvall.supportmanagement.api.filter;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zalando.problem.Problem;

@Component
public class SentByHeaderFilter extends OncePerRequestFilter {

	public static final ThreadLocal<String> SENT_BY_IDENTIFIER = new ThreadLocal<>();
	public static final ThreadLocal<String> SENT_BY_TYPE = new ThreadLocal<>();

	public String getSenderType() {
		return SENT_BY_TYPE.get();
	}

	public String getSenderId() {
		return SENT_BY_IDENTIFIER.get();
	}

	@Override
	protected void doFilterInternal(
		@NotNull final HttpServletRequest request,
		@NotNull final HttpServletResponse response,
		@NotNull final FilterChain filterChain) throws ServletException, IOException {

		String headerValue = request.getHeader(SENT_BY_HEADER);
		if (headerValue == null) {
			filterChain.doFilter(request, response);
			return;
		}

		String[] parts = validateTwoParts(headerValue);
		var type = parts[1];
		var identifier = parts[0];

		type = validateType(type);
		validateIdentifier(type, identifier);

		SENT_BY_TYPE.set(type);
		SENT_BY_IDENTIFIER.set(identifier);

		try {
			filterChain.doFilter(request, response);
		} finally {
			// Remove values after request is processed. This is important to avoid leaking values between requests.
			SENT_BY_IDENTIFIER.remove();
			SENT_BY_TYPE.remove();
		}
	}

	/**
	 * Validates that the value has two parts separated by a colon
	 *
	 * @param  value the X-Sent-By header value
	 * @return       the two parts
	 */
	String[] validateTwoParts(final String value) {
		var parts = value.split(";");
		if (parts.length != 2) {
			throw Problem.valueOf(BAD_REQUEST, "Invalid X-Sent-By header value");
		}
		return parts;
	}

	/**
	 * Validates that the type is either partyId or adAccount
	 *
	 * @param type the type value
	 */
	String validateType(final String type) {
		if (!type.trim().startsWith("type=")) {
			throw Problem.valueOf(BAD_REQUEST, "Invalid X-Sent-By type value");
		}
		var typeValue = type.trim().substring(5);

		if ("partyId".equals(typeValue)) {
			return typeValue;
		}
		if ("adAccount".equals(typeValue)) {
			return typeValue;
		}
		throw Problem.valueOf(BAD_REQUEST, "Invalid X-Sent-By type value");
	}

	/**
	 * Validates the identifier based on the type
	 *
	 * @param type       the type
	 * @param identifier the identifier
	 */
	void validateIdentifier(final String type, final String identifier) {
		if ("partyId".equals(type)) {
			try {
				UUID.fromString(identifier);
			} catch (IllegalArgumentException e) {
				throw Problem.valueOf(BAD_REQUEST, "Party id identifier must be a valid UUID");
			}
		} else if ("adAccount".equals(type)) {
			if (identifier.isBlank()) {
				throw Problem.valueOf(BAD_REQUEST, "Ad account identifier cannot be blank");
			}
		} else {
			// Type is validated earlier and is either partyId or adAccount
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Should not be possible to reach this point");
		}
	}
}
