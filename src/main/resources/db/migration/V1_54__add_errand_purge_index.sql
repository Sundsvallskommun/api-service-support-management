-- The retention purge walks a namespace in id order, a batch at a time, asking for the errands that come after the id
-- the previous batch ended on. Without an index leading with municipality_id and namespace and ending in id, finding
-- where the previous batch ended costs a pass over the namespace for every batch.
create index if not exists idx_errand_municipality_id_namespace_id
    on errand (municipality_id, namespace, id);
