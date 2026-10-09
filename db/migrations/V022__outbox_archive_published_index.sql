create index if not exists idx_outbox_published_at
  on outbox_event(published_at, id)
  where published_at is not null;
