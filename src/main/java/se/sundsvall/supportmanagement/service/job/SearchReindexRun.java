package se.sundsvall.supportmanagement.service.job;

/**
 * A rebuild of the search index of one namespace on its way to the thread that carries it out.
 *
 * @param jobId          id of the job the rebuild reports its progress against.
 * @param namespace      namespace to rebuild.
 * @param municipalityId id of the municipality the namespace belongs to.
 */
public record SearchReindexRun(String jobId, String namespace, String municipalityId) {
}
