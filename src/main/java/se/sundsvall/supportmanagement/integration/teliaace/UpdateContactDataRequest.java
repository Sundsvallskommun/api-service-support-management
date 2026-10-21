package se.sundsvall.supportmanagement.integration.teliaace;

import java.util.Map;

/**
 * Request body for Telia ACE WorkItemInterfaceProxy's {@code PUT /workitem/emailUid/{emailUid}} (UpdateContactData),
 * see InterfaceSpecification_WorkItemInterfaceProxy_REST_rev8.0.pdf §3.5. Only sets the contact data keys given -
 * there is no append operation, so a key is set to the full value it should hold next.
 */
public record UpdateContactDataRequest(Map<String, String> customKeys) {
}
