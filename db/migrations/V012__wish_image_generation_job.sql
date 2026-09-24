create table wish_image_generation_job (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  wish_id uuid references wish(id),
  usage_code text not null default 'wish_card',
  provider_code text references image_model_provider(code),
  title_snapshot text not null,
  note_snapshot text,
  category text,
  style text not null default 'warm_illustration'
    check (style in ('warm_illustration', 'storybook', 'clean_product')),
  aspect_ratio text not null default '1:1'
    check (aspect_ratio in ('1:1', '4:3')),
  status text not null default 'queued'
    check (status in ('queued', 'running', 'succeeded', 'failed_retryable', 'failed_final', 'cancelled')),
  media_asset_id uuid references media_asset(id),
  model_provider text,
  model_name text,
  prompt text,
  attempt_count int not null default 0,
  available_at timestamptz not null default now(),
  leased_until timestamptz,
  error_code text,
  error_message text,
  created_by uuid not null references auth_user(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_wish_image_generation_claim
  on wish_image_generation_job(status, available_at, leased_until)
  where status in ('queued', 'failed_retryable');

create index idx_wish_image_generation_family_child_created
  on wish_image_generation_job(family_id, child_id, created_at desc);
