package se.sundsvall.supportmanagement.service.model;

/**
 * A request aimed straight at the process rather than something that happened to the errand: a handler starting the
 * handling by hand, or stepping a process past a gate it waits at.
 * <p>
 * When the event is published, the chosen key goes before every other way of resolving one, the labels included.
 *
 * @param processKey the process the handler chose to start, or null for a command that starts nothing.
 * @param signalName the gate the handler pressed, which is what the process engine correlates on. Null unless the
 *                   command is a signal.
 */
public record ProcessCommand(String processKey, String signalName) {}
