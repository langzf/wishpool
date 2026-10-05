alter table device add column revoked_at timestamptz null;
alter table device add column revoked_by_user_id uuid null references auth_user(id);
create index idx_device_family_revoked on device(family_id, revoked_at);
