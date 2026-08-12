alter table media_asset
  add column processing_attempt_count int not null default 0,
  add column processing_available_at timestamptz not null default now(),
  add column processing_leased_until timestamptz,
  add column processing_retryable boolean not null default true,
  add column processing_last_error_code text,
  add column processing_last_error_message text;

create index idx_media_processing_claim
  on media_asset(status, processing_available_at, processing_leased_until, created_at)
  where status in ('uploaded', 'processing', 'failed');
