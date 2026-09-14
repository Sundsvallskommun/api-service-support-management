package se.sundsvall.supportmanagement.service.model;

/**
 * A request aimed straight at the process rather than something that happened to the errand: a handler starting the
 * handling by hand, or stepping a process past a gate it waits at.
 * <p>
 * What a command carries is what publication may not work out for itself. The chosen key goes before every other way of
 * resolving one, since the handler has already chosen and resolving it again from the labels would drop the very
 * command that exists to settle an ambiguity.
 *
 * @param processKey the process the handler chose to start, or null for a command that starts nothing.
 * @param signalName the gate the handler pressed, which is what the process engine correlates on. Null unless the
 *                   command is a signal.
 */
public record ProcessCommand(String processKey, String signalName) {}
