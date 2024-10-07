alter table if exists notification 
   add constraint fk_notification_errand_id 
   foreign key (errand_id) 
   references errand (id);