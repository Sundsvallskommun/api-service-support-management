package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.emailreader.Email;
import java.util.function.Consumer;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

/**
 * Requests that an errand be relayed to Telia ACE as a work item. Handled by
 * {@link TeliaAceWorkItemService#onTeliaAceNotificationRequested} after the publishing transaction commits.
 *
 * @param errand               the errand to notify Telia ACE about
 * @param email                the email that triggered the notification
 * @param setUnHealthyConsumer signal for the email reader job's health; carried on the event because the listener
 *                             runs after {@code processEmail} has already returned to its caller
 */
public record TeliaAceNotificationRequestedEvent(ErrandEntity errand, Email email, Consumer<String> setUnHealthyConsumer) {}
