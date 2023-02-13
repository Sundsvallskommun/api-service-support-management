alter table errand
  add resolution varchar(255)
    after reporter_user_id,
  add description varchar(5000)
    after customer_type;