package se.sundsvall.supportmanagement.service.purge;

/**
 * A purge run on its way to the thread that carries it out.
 * <p>
 * Everything the run needs to know is settled when it is accepted: which job it reports against, where it walks, who
 * asked for it and what it was told to do. It carries the identity of the caller from the request thread to the thread
 * the run is carried out on.
 *
 * @param jobId          id of the job the run reports its progress against.
 * @param namespace      namespace to purge within.
 * @param municipalityId id of the municipality to purge within.
 * @param startedBy      who asked for the run.
 * @param settings       what the run was told to do.
 */
public record PurgeRun(String jobId, String namespace, String municipalityId, String startedBy, PurgeSettings settings) {
}
