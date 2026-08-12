create table phone_verification_code (
  id uuid primary key default gen_random_uuid(),
  phone_number text not null,
  purpose text not null check (purpose in ('login')),
  verification_token_hash text not null unique,
  code_hash text not null,
  status text not null default 'active' check (status in ('active', 'consumed', 'expired')),
  attempt_count int not null default 0,
  expires_at timestamptz not null,
  consumed_at timestamptz,
  created_at timestamptz not null default now()
);

create index idx_phone_verification_phone_status on phone_verification_code(phone_number, status, created_at desc);
create index idx_phone_verification_expires on phone_verification_code(expires_at);
