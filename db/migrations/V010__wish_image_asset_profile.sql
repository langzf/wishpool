create table wish_image_asset_profile (
  media_asset_id uuid primary key references media_asset(id) on delete cascade,
  family_id uuid not null references family(id),
  child_id uuid references child_profile(id),
  source_type text not null default 'uploaded' check (source_type in ('uploaded', 'generated', 'reused', 'imported')),
  source_wish_id uuid references wish(id),
  title_snapshot text not null,
  note_snapshot text,
  normalized_title text not null,
  keywords text[] not null default '{}',
  category text,
  tags text[] not null default '{}',
  similarity_key text,
  prompt text,
  provider text,
  model text,
  reuse_allowed boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_wish_image_profile_family_child_created
  on wish_image_asset_profile(family_id, child_id, created_at desc);

create index idx_wish_image_profile_normalized_title
  on wish_image_asset_profile(family_id, normalized_title);

create index idx_wish_image_profile_keywords_gin
  on wish_image_asset_profile using gin(keywords);

create index idx_wish_image_profile_tags_gin
  on wish_image_asset_profile using gin(tags);

alter table wish
  add column fragment_visual_mode text not null default 'grid_reveal'
    check (fragment_visual_mode in ('grid_reveal', 'puzzle_lines', 'irregular')),
  add column fragment_grid_rows int check (fragment_grid_rows is null or fragment_grid_rows between 1 and 4),
  add column fragment_grid_cols int check (fragment_grid_cols is null or fragment_grid_cols between 1 and 4);
