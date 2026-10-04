alter table notification_event
  add column error_message text null,
  add column provider_message_id text null;
