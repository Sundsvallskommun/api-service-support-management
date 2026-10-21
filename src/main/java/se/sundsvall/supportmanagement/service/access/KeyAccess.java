package se.sundsvall.supportmanagement.service.access;

import java.util.function.Predicate;

/**
 * What a caller may do with the keys of one field of one errand.
 *
 * @param readableKey the keys they may see
 * @param writableKey the keys they may change, never wider than the ones they may see
 */
public record KeyAccess(Predicate<String> readableKey, Predicate<String> writableKey) {}
