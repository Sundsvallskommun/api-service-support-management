alter table errand
  add resolution varchar(255)
    after reporter_user_id,
  add description longtext
    after customer_type;