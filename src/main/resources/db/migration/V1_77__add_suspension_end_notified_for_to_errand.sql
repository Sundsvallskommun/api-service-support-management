alter table errand
    add column if not exists suspension_end_notified_for datetime(6) null;
