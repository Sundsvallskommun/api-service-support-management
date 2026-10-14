package se.sundsvall.supportmanagement.integration.teliaace;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Request body for Telia ACE WorkItemInterfaceProxy's {@code POST /workitem} (AddWorkItem), see
 * InterfaceSpecification_WorkItemInterfaceProxy_REST_rev8.0.pdf §3.1. Creates a new, unkeyed work item - the
 * {@code emailUid} the response carries is what later identifies it to {@link UpdateContactDataRequest}.
 */
@JsonInclude(NON_NULL)
public record AddWorkItemRequest(
	String from,
	String subject,
	String entrance,
	String errand,
	String workitem,
	String workitemSource,
	String contentUrl,
	Map<String, String> customKeys) {
}
