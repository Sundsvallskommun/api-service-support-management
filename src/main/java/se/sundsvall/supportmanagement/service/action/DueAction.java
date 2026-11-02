package se.sundsvall.supportmanagement.service.action;

import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;

/**
 * An action due at once, left to be carried out once the change that called for it has been recorded.
 *
 * @param action the action to carry out
 * @param config the configuration it is carried out by
 */
public record DueAction(Action action, ActionConfigEntity config) {}
