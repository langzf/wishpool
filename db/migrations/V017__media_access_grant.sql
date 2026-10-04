create table media_access_grant (
  id uuid primary key,
  family_id uuid not null references family(id),
  media_asset_id uuid not null references media_asset(id),
  granted_by_actor_role varchar(64) not null,
  reason varchar(500) not null,
  expires_at timestamptz not null,
  revoked_at timestamptz null,
  revoked_reason varchar(500) null,
  audit_log_id uuid not null references audit_log(id),
  created_at timestamptz not null default now()
);
create index idx_media_access_grant_family_created on media_access_grant(family_id, created_at desc);
create index idx_media_access_grant_asset_created on media_access_grant(media_asset_id, created_at desc);
create index idx_media_access_grant_created on media_access_grant(created_at desc);
