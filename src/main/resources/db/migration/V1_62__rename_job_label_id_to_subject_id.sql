-- Renamed from label_id: this column now carries the subject of any job that
-- centers on one - not only a moved label - so a label-move-specific name on
-- a table shared with every other kind of job no longer fits.
alter table job
    change column label_id subject_id varchar(255);
