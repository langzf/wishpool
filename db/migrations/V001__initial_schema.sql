create extension if not exists pgcrypto;

create table auth_user (
  id uuid primary key default gen_random_uuid(),
  status text not null check (status in ('active', 'suspended', 'deleted')),
  display_name text not null,
  avatar_url text,
  last_login_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_auth_user_status on auth_user(status);

create table auth_provider_identity (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth_user(id),
  provider text not null check (provider in ('phone', 'wechat')),
  provider_subject text not null,
  verified_at timestamptz,
  created_at timestamptz not null default now(),
  unique (provider, provider_subject),
  unique (user_id, provider)
);

create table admin_user_role (
  user_id uuid not null references auth_user(id),
  role text not null check (role in ('admin_support', 'admin_super')),
  granted_by uuid not null references auth_user(id),
  granted_at timestamptz not null default now(),
  revoked_at timestamptz,
  primary key (user_id, role)
);

create table family (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  timezone text not null default 'Asia/Shanghai',
  owner_user_id uuid not null references auth_user(id),
  status text not null default 'active' check (status in ('active', 'locked', 'deleting', 'deleted')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_family_owner on family(owner_user_id);
create index idx_family_status on family(status);

create table child_profile (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  nickname text not null,
  birth_year int,
  avatar_asset text,
  room_theme text not null default 'star_cabin',
  mode_lock_hash text,
  status text not null default 'active' check (status in ('active', 'archived')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_child_profile_family_status on child_profile(family_id, status);

create table family_member (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  user_id uuid not null references auth_user(id),
  role text not null check (role in ('parent_owner', 'parent', 'child_device')),
  child_id uuid references child_profile(id),
  display_name text not null,
  status text not null default 'active' check (status in ('active', 'invited', 'suspended', 'removed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ck_family_member_child_role check (
    (role = 'child_device' and child_id is not null)
    or (role in ('parent_owner', 'parent') and child_id is null)
  )
);

create unique index uk_family_member_active_user on family_member(family_id, user_id) where status != 'removed';
create index idx_family_member_family_role on family_member(family_id, role);
create index idx_family_member_user on family_member(user_id);
create index idx_family_member_child on family_member(child_id);

create table device (
  id uuid primary key default gen_random_uuid(),
  family_id uuid references family(id),
  user_id uuid not null references auth_user(id),
  child_id uuid references child_profile(id),
  platform text not null check (platform in ('ios', 'android', 'ipad_os', 'web', 'admin_web')),
  device_name text,
  push_token text,
  push_provider text check (push_provider is null or push_provider in ('apns', 'fcm', 'huawei', 'xiaomi', 'oppo', 'vivo')),
  last_seen_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_device_user on device(user_id);
create index idx_device_family_child on device(family_id, child_id);
create index idx_device_push_token on device(push_provider, push_token);

create table auth_session (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth_user(id),
  device_id uuid references device(id),
  refresh_token_hash text not null unique,
  status text not null default 'active' check (status in ('active', 'revoked', 'expired')),
  issued_at timestamptz not null default now(),
  expires_at timestamptz not null,
  revoked_at timestamptz,
  created_at timestamptz not null default now()
);

create index idx_auth_session_user_status on auth_session(user_id, status);
create index idx_auth_session_expires on auth_session(expires_at);

create table family_invite (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  invited_by uuid not null references auth_user(id),
  invitee_contact_hash text,
  role text not null default 'parent' check (role = 'parent'),
  token_hash text not null unique,
  status text not null default 'pending' check (status in ('pending', 'accepted', 'expired', 'revoked')),
  expires_at timestamptz not null,
  accepted_by uuid references auth_user(id),
  accepted_at timestamptz,
  created_at timestamptz not null default now()
);

create index idx_family_invite_family_status on family_invite(family_id, status);

create table pairing_session (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  created_by uuid not null references auth_user(id),
  code_hash text not null,
  status text not null default 'active' check (status in ('active', 'consumed', 'expired', 'revoked')),
  expires_at timestamptz not null,
  consumed_by_user_id uuid references auth_user(id),
  consumed_device_id uuid references device(id),
  consumed_at timestamptz,
  created_at timestamptz not null default now()
);

create index idx_pairing_family_child_status on pairing_session(family_id, child_id, status);
create index idx_pairing_expires on pairing_session(expires_at);

create table task_template (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  title text not null,
  category text not null check (category in ('study', 'reading', 'exercise', 'habit', 'custom')),
  submission_type text not null check (submission_type in ('photo', 'audio', 'video', 'manual')),
  description text,
  target_text text,
  default_duration_sec int,
  created_by uuid not null references auth_user(id),
  archived_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_task_template_family_category on task_template(family_id, category);
create index idx_task_template_active on task_template(family_id) where archived_at is null;

create table wish (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  week_id text not null,
  title text not null,
  note text,
  image_media_id uuid,
  required_fragments int not null check (required_fragments > 0),
  earned_fragments int not null default 0 check (earned_fragments >= 0),
  reward_mode text not null default 'flexible' check (reward_mode in ('flexible', 'strict')),
  status text not null default 'draft' check (status in ('draft', 'active', 'unlocked', 'redeemed', 'archived', 'cancelled', 'cancelled_by_parent')),
  created_by uuid not null references auth_user(id),
  unlocked_at timestamptz,
  redeemed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index uk_active_wish_week on wish(family_id, child_id, week_id)
  where status in ('active', 'unlocked', 'redeemed');

create table weekly_plan (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  week_id text not null,
  start_date date not null,
  end_date date not null,
  wish_id uuid references wish(id),
  reward_mode text not null default 'flexible' check (reward_mode in ('flexible', 'strict')),
  status text not null default 'draft' check (status in ('draft', 'active', 'archived')),
  version bigint not null default 0,
  created_by uuid not null references auth_user(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (family_id, child_id, week_id),
  constraint ck_weekly_plan_dates check (start_date <= end_date)
);

create table weekly_plan_rule (
  id uuid primary key default gen_random_uuid(),
  weekly_plan_id uuid not null references weekly_plan(id) on delete cascade,
  task_template_id uuid references task_template(id),
  title_snapshot text not null,
  category text not null check (category in ('study', 'reading', 'exercise', 'habit', 'custom')),
  submission_type text not null check (submission_type in ('photo', 'audio', 'video', 'manual')),
  description_snapshot text,
  target_text_snapshot text,
  weekdays int[] not null,
  is_core boolean not null default true,
  require_review boolean not null default true,
  sort_order int not null default 0,
  created_at timestamptz not null default now(),
  constraint ck_weekly_plan_rule_weekdays check (weekdays <@ array[1,2,3,4,5,6,7])
);

create table task_instance (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  weekly_plan_id uuid references weekly_plan(id),
  plan_rule_id uuid references weekly_plan_rule(id),
  scheduled_date date not null,
  source text not null check (source in ('weekly_rule', 'adhoc', 'carry_over')),
  title text not null,
  category text not null check (category in ('study', 'reading', 'exercise', 'habit', 'custom')),
  submission_type text not null check (submission_type in ('photo', 'audio', 'video', 'manual')),
  description text,
  target_text text,
  is_core boolean not null default true,
  require_review boolean not null default true,
  status text not null default 'todo' check (status in ('todo', 'submitted', 'ai_processing', 'pending_review', 'approved', 'needs_revision', 'skipped', 'expired', 'adjusted_by_parent')),
  latest_submission_id uuid,
  approved_review_id uuid,
  skip_reason text,
  sort_order int not null default 0,
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_task_child_date on task_instance(child_id, scheduled_date, sort_order);
create index idx_task_family_date_status on task_instance(family_id, scheduled_date, status);
create index idx_task_review_queue on task_instance(family_id, status, updated_at desc) where status = 'pending_review';
create unique index uk_task_rule_date on task_instance(child_id, scheduled_date, plan_rule_id) where plan_rule_id is not null;

create table media_asset (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid references child_profile(id),
  purpose text not null check (purpose in ('submission', 'feedback', 'wish_redemption', 'memory_export')),
  storage_key text not null unique,
  content_type text not null,
  size_bytes bigint,
  duration_sec int,
  width int,
  height int,
  checksum_sha256 text,
  status text not null default 'upload_pending' check (status in ('upload_pending', 'uploaded', 'processing', 'ready', 'failed', 'deleted')),
  created_by uuid not null references auth_user(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_media_family_child on media_asset(family_id, child_id, created_at desc);
create index idx_media_status on media_asset(status, created_at);

alter table wish
  add constraint fk_wish_image_media foreign key (image_media_id) references media_asset(id);

create table submission (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  task_instance_id uuid not null references task_instance(id),
  attempt_no int not null,
  submission_type text not null check (submission_type in ('photo', 'audio', 'video', 'manual')),
  status text not null default 'created' check (status in ('created', 'media_uploaded', 'ai_pending', 'ai_processing', 'review_pending', 'approved', 'rejected', 'superseded', 'cancelled')),
  client_mutation_id text not null,
  submitted_by_device_id uuid not null references device(id),
  submitted_at timestamptz not null default now(),
  superseded_by_submission_id uuid references submission(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (family_id, client_mutation_id),
  unique (task_instance_id, attempt_no)
);

create index idx_submission_task_attempt on submission(task_instance_id, attempt_no desc);
create index idx_submission_review_queue on submission(family_id, status, submitted_at desc) where status = 'review_pending';
create index idx_submission_child_date on submission(child_id, submitted_at desc);

alter table task_instance
  add constraint fk_task_latest_submission foreign key (latest_submission_id) references submission(id);

create table submission_media (
  submission_id uuid not null references submission(id) on delete cascade,
  media_asset_id uuid not null references media_asset(id),
  sort_order int not null default 0,
  primary key (submission_id, media_asset_id)
);

create table ai_job (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  submission_id uuid references submission(id),
  job_type text not null check (job_type in ('image_homework_precheck', 'reading_audio_precheck', 'exercise_video_precheck', 'content_safety', 'memory_summary')),
  status text not null default 'queued' check (status in ('queued', 'media_preparing', 'running', 'succeeded', 'failed_retryable', 'failed_final', 'cancelled')),
  model_route text,
  attempt_count int not null default 0,
  error_code text,
  error_message text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_ai_job_status_created on ai_job(status, created_at);
create index idx_ai_job_submission on ai_job(submission_id);

create table ai_precheck (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  ai_job_id uuid not null references ai_job(id),
  submission_id uuid not null references submission(id),
  type text not null,
  summary text not null,
  confidence numeric(4,3),
  flags_json jsonb not null default '[]',
  raw_text_ref text,
  model_provider text not null,
  model_name text not null,
  model_version text not null,
  prompt_version text,
  created_at timestamptz not null default now(),
  unique (submission_id)
);

create table model_invocation_log (
  id uuid primary key default gen_random_uuid(),
  ai_job_id uuid not null references ai_job(id),
  provider text not null,
  model text not null,
  latency_ms int,
  cost_units numeric(12,4),
  status text not null check (status in ('succeeded', 'failed')),
  error_code text,
  created_at timestamptz not null default now()
);

create table review (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  submission_id uuid not null references submission(id),
  task_instance_id uuid not null references task_instance(id),
  decision text not null check (decision in ('approved', 'needs_revision')),
  reviewed_by uuid not null references auth_user(id),
  created_at timestamptz not null default now(),
  revoked_at timestamptz,
  revoke_reason text
);

create unique index uk_review_submission_active on review(submission_id) where revoked_at is null;

alter table task_instance
  add constraint fk_task_approved_review foreign key (approved_review_id) references review(id);

create table feedback (
  id uuid primary key default gen_random_uuid(),
  review_id uuid not null references review(id),
  emoji text,
  text text,
  audio_media_id uuid references media_asset(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table daily_summary (
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  date date not null,
  week_id text not null,
  core_required int not null default 0,
  core_approved int not null default 0,
  core_skipped int not null default 0,
  total_tasks int not null default 0,
  approved_tasks int not null default 0,
  fragment_status text not null default 'not_earned' check (fragment_status in ('not_eligible', 'not_earned', 'earned', 'makeup_available', 'makeup_earned', 'adjusted')),
  fragment_reward_id uuid,
  updated_at timestamptz not null default now(),
  primary key (family_id, child_id, date)
);

create table reward_ledger (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  week_id text,
  date date,
  reward_type text not null check (reward_type in ('star_light', 'wish_fragment', 'adjustment')),
  reason text not null,
  amount int not null,
  wish_id uuid references wish(id),
  task_instance_id uuid references task_instance(id),
  review_id uuid references review(id),
  source_reward_id uuid references reward_ledger(id),
  idempotency_key text not null unique,
  created_by uuid references auth_user(id),
  created_at timestamptz not null default now()
);

create index idx_reward_child_week on reward_ledger(child_id, week_id, reward_type);
create index idx_reward_wish on reward_ledger(wish_id);
create index idx_reward_task on reward_ledger(task_instance_id);

alter table daily_summary
  add constraint fk_daily_summary_fragment_reward foreign key (fragment_reward_id) references reward_ledger(id);

create table wish_redemption (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  wish_id uuid not null unique references wish(id),
  redeemed_by uuid not null references auth_user(id),
  redeemed_date date not null,
  parent_note text,
  child_note text,
  created_at timestamptz not null default now()
);

create table wish_redemption_media (
  wish_redemption_id uuid not null references wish_redemption(id) on delete cascade,
  media_asset_id uuid not null references media_asset(id),
  sort_order int not null default 0,
  primary key (wish_redemption_id, media_asset_id)
);

create table weekly_memory (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  week_id text not null,
  start_date date not null,
  end_date date not null,
  wish_id uuid references wish(id),
  title text not null,
  summary_json jsonb not null default '{}',
  status text not null default 'generating' check (status in ('generating', 'generated', 'exported')),
  generated_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (family_id, child_id, week_id)
);

create index idx_weekly_memory_child_start on weekly_memory(child_id, start_date desc);

create table memory_item (
  id uuid primary key default gen_random_uuid(),
  weekly_memory_id uuid not null references weekly_memory(id) on delete cascade,
  item_type text not null,
  source_type text,
  source_id uuid,
  media_asset_id uuid references media_asset(id),
  content_json jsonb not null default '{}',
  sort_order int not null default 0,
  created_at timestamptz not null default now()
);

create table room_item (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  child_id uuid not null references child_profile(id),
  type text not null,
  source_type text not null,
  source_id uuid,
  title text not null,
  media_asset_id uuid references media_asset(id),
  position_json jsonb not null default '{}',
  visible boolean not null default true,
  unlocked_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index idx_room_item_child_visible on room_item(child_id, visible, unlocked_at desc);
create unique index uk_room_item_source on room_item(child_id, type, source_type, source_id) where source_id is not null;

create table media_derivative (
  id uuid primary key default gen_random_uuid(),
  media_asset_id uuid not null references media_asset(id) on delete cascade,
  kind text not null,
  storage_key text not null unique,
  content_type text not null,
  size_bytes bigint,
  metadata_json jsonb not null default '{}',
  created_at timestamptz not null default now(),
  unique (media_asset_id, kind)
);

create table family_event (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  seq bigint not null,
  event_type text not null,
  aggregate_type text not null,
  aggregate_id uuid not null,
  payload_json jsonb not null,
  created_at timestamptz not null default now(),
  unique (family_id, seq)
);

create index idx_family_event_pull on family_event(family_id, seq);
create index idx_family_event_type on family_event(family_id, event_type, created_at desc);

create table outbox_event (
  id uuid primary key default gen_random_uuid(),
  event_type text not null,
  aggregate_type text not null,
  aggregate_id uuid not null,
  payload_json jsonb not null,
  available_at timestamptz not null default now(),
  published_at timestamptz,
  retry_count int not null default 0,
  created_at timestamptz not null default now()
);

create index idx_outbox_unpublished on outbox_event(available_at, created_at) where published_at is null;

create table notification_event (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  recipient_user_id uuid not null references auth_user(id),
  recipient_device_id uuid references device(id),
  type text not null,
  title text not null,
  body text not null,
  related_resource_type text,
  related_resource_id uuid,
  status text not null default 'pending' check (status in ('pending', 'sent', 'failed', 'read', 'suppressed')),
  dedupe_key text,
  sent_at timestamptz,
  read_at timestamptz,
  created_at timestamptz not null default now()
);

create unique index uk_notification_dedupe on notification_event(dedupe_key) where dedupe_key is not null;

create table notification_preference (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  user_id uuid not null references auth_user(id),
  notification_type text not null,
  enabled boolean not null default true,
  quiet_hours_json jsonb not null default '{}',
  channels_json jsonb not null default '{}',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (family_id, user_id, notification_type)
);

create table audit_log (
  id uuid primary key default gen_random_uuid(),
  family_id uuid references family(id),
  actor_user_id uuid references auth_user(id),
  actor_role text not null,
  action text not null,
  resource_type text not null,
  resource_id uuid,
  metadata_json jsonb not null default '{}',
  ip_hash text,
  user_agent text,
  created_at timestamptz not null default now()
);

create index idx_audit_family_time on audit_log(family_id, created_at desc);
create index idx_audit_actor_time on audit_log(actor_user_id, created_at desc);
create index idx_audit_action_time on audit_log(action, created_at desc);

create table privacy_request (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references family(id),
  request_type text not null check (request_type in ('export', 'delete')),
  status text not null default 'requested' check (status in ('requested', 'verifying', 'locking_family', 'deleting_records', 'deleting_objects', 'verifying_deletion', 'completed', 'failed_needs_attention')),
  requested_by uuid not null references auth_user(id),
  export_media_id uuid references media_asset(id),
  reason text,
  created_at timestamptz not null default now(),
  completed_at timestamptz,
  error_message text
);
