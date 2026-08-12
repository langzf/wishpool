alter table media_asset
  drop constraint media_asset_purpose_check;

alter table media_asset
  add constraint media_asset_purpose_check
  check (purpose in ('submission', 'feedback', 'wish_image', 'wish_redemption', 'memory_export'));
