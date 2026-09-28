package se.sundsvall.supportmanagement.service;

import se.sundsvall.supportmanagement.integration.db.model.enums.JobType;

/**
 * What a job being launched is, before any run is dispatched against it - everything {@link JobService#launch} needs
 * to create the job's row via {@link JobService#create}, grouped here rather than passed as parameters of its own so
 * that {@code launch} does not carry the job's shape and the run's dispatch as nine parameters of its own.
 *
 * @param namespace      namespace the job belongs to.
 * @param municipalityId id of the municipality the job belongs to.
 * @param type           the kind of job to create.
 * @param total          what the job's progress is measured against.
 * @param subjectId      id of whatever single thing the job centers on, or {@code null} if it does not center on one.
 */
public record JobSpec(String namespace, String municipalityId, JobType type, int total, String subjectId) {
}
