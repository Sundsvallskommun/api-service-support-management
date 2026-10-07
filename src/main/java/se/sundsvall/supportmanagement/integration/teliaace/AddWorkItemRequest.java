package se.sundsvall.supportmanagement.integration.teliaace;

import java.util.Map;

/**
 * Request body for Telia ACE WorkItemInterfaceProxy's {@code POST /workitem} (AddWorkItem), see
 * InterfaceSpecification_WorkItemInterfaceProxy_REST_rev8.0.pdf §3.1.
 */
public record AddWorkItemRequest(
	String from,
	String subject,
	String entrance,
	String errand,
	String workitem,
	String contentUrl,
	Map<String, String> customKeys) {
}
