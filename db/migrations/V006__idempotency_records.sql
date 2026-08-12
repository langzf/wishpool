create table idempotency_record (
  family_id uuid not null references family(id),
  key text not null,
  operation text not null,
  resource_type text not null,
  resource_id uuid not null,
  created_by uuid not null references auth_user(id),
  created_at timestamptz not null default now(),
  primary key (family_id, key, operation)
);

create index idx_idempotency_resource
  on idempotency_record(resource_type, resource_id);
