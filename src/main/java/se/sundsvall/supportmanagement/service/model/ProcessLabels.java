package se.sundsvall.supportmanagement.service.model;

/**
 * What the labels of an errand say about processes, read out of them at once.
 *
 * @param blocked   whether a label of the errand blocks processes.
 * @param selection which process the labels select, and with which start mode.
 */
public record ProcessLabels(boolean blocked, ProcessKeySelection selection) {
}
