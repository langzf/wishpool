alter table outbox_event
  add column leased_until timestamptz,
  add column last_error text;

create index idx_outbox_lease
  on outbox_event(leased_until)
  where published_at is null and leased_until is not null;
