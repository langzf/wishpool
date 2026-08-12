alter table media_asset
  add column related_type text,
  add column related_id uuid;

create index idx_media_asset_related_resource
  on media_asset(family_id, related_type, related_id)
  where related_type is not null and related_id is not null;
