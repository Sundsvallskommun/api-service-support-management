package se.sundsvall.supportmanagement.service.access;

import generated.se.sundsvall.accessmapper.Access;
import java.util.Map;

/**
 * What a user may do with one field of one errand. A field they do not reach at all is simply absent.
 *
 * @param level   what they may do with the field itself, which the errand answers for every field written through
 *                it and the resource serving it answers for the rest
 * @param allKeys if the field is reached without a key restriction, null for a field holding no keyed collection
 * @param keys    every key of the collection they reach, empty when {@code allKeys}, null for a field holding no
 *                keyed collection
 */
public record FieldGrant(
	Access.AccessLevelEnum level,
	Boolean allKeys,
	Map<String, Access.AccessLevelEnum> keys) {}
