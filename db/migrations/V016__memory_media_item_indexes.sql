-- Memory media items are rebuilt from approved submissions and queried by asset during playback.
create index idx_memory_item_media_asset on memory_item(media_asset_id) where media_asset_id is not null;
