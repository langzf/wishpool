alter table task_instance
  add column original_task_instance_id uuid references task_instance(id);

create index idx_task_original_instance on task_instance(original_task_instance_id)
  where original_task_instance_id is not null;
