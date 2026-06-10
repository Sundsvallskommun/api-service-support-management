-- Remove time_measurement rows that were orphaned by status changes persisted directly via the repository.
-- Before TimeMeasurementEntity owned the errand_id foreign key, the unidirectional @OneToMany @JoinColumn
-- wrote the FK only during collection flushing, so listener-created measures could be inserted with a null
-- errand_id when a status change was saved outside ErrandService.updateErrand (e.g. EmailReaderWorker,
-- MessageExchangeSyncService).
delete from time_measurement where errand_id is null;

-- errand_id is now always set on insert; enforce it at the schema level.
-- The foreign key constraint fk_errand_time_measure_errand_id already exists (added in V1_0).
alter table time_measurement modify errand_id varchar(255) not null;
