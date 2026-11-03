-- The removal of soft deleted errands walks every namespace that sets how long it keeps them, asking for the errands
-- soft deleted before a point in time. Without an index leading with municipality_id and namespace and ending in deleted,
-- every run costs a pass over each such namespace, however few of its errands are soft deleted.
create index if not exists idx_errand_municipality_id_namespace_deleted
    on errand (municipality_id, namespace, deleted);
