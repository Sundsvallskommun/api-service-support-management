package se.sundsvall.supportmanagement.service.model;

import java.util.Set;
import se.sundsvall.dept44.support.Identifier;

/**
 * What goes along with an errand event beyond the event itself.
 *
 * @param sendNotification whether the users notified directly of a change to the errand are notified of this one
 * @param actor            the user who acted, or null when nobody did
 * @param addedLabelIds    the metadata labels the event added to the errand, which subscribers may filter on
 */
public record ErrandEventOptions(boolean sendNotification, Identifier actor, Set<String> addedLabelIds) {}
