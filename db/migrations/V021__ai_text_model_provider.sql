create table ai_text_model_provider (
    id uuid primary key default gen_random_uuid(),
    code varchar(100) not null unique,
    display_name varchar(200) not null,
    provider_type varchar(40) not null check (provider_type in ('volcengine_ark','aliyun_bailian','openai_compatible','deepseek','deterministic')),
    base_url text not null,
    api_key text,
    model_name varchar(200) not null,
    capability varchar(20) not null check (capability in ('text','vision','text_vision','asr')),
    extra_params jsonb not null default '{}'::jsonb,
    is_default boolean not null default false,
    is_enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create unique index uq_ai_text_model_provider_default_capability
    on ai_text_model_provider(capability) where is_default;
create index idx_ai_text_model_provider_capability_enabled
    on ai_text_model_provider(capability, is_enabled);
