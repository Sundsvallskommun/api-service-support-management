    create index idx_errand_status 
       on errand (status);

    create index idx_errand_category 
       on errand (category);

    create index idx_errand_type 
       on errand (type);

    create index idx_errand_assigned_user_id 
       on errand (assigned_user_id);

    create index idx_errand_reporter_user_id 
       on errand (reporter_user_id);