alter table media_asset add column processing_lease_token text;
create index idx_media_processing_lease_token on media_asset(processing_lease_token) where processing_lease_token is not null;
