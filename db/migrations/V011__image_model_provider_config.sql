create table image_model_provider (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  display_name text not null,
  provider_type text not null check (
    provider_type in (
      'volcengine_ark',
      'aliyun_bailian',
      'siliconflow',
      'deterministic',
      'custom_openai_compatible'
    )
  ),
  base_url text not null,
  api_key text,
  model_name text not null,
  extra_params jsonb not null default '{}'::jsonb,
  is_default boolean not null default false,
  is_enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint image_model_provider_code_format check (code ~ '^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$')
);

create unique index ux_image_model_provider_single_default
  on image_model_provider(is_default)
  where is_default = true;

create index idx_image_model_provider_enabled
  on image_model_provider(is_enabled, is_default desc, updated_at desc);

create table image_gen_usage (
  usage_code text primary key,
  provider_code text not null references image_model_provider(code),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint image_gen_usage_code_format check (usage_code ~ '^[a-z0-9][a-z0-9_]{1,62}[a-z0-9]$')
);
