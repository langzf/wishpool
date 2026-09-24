alter table wish
  add column fragment_mask_json jsonb,
  add column fragment_lit_json jsonb not null default '{"version": 1, "litIndexes": []}'::jsonb;

alter table wish
  add constraint wish_fragment_mask_json_object_check
    check (fragment_mask_json is null or jsonb_typeof(fragment_mask_json) = 'object'),
  add constraint wish_fragment_lit_json_object_check
    check (jsonb_typeof(fragment_lit_json) = 'object');

with normalized as (
  select
    w.id,
    w.required_fragments,
    w.earned_fragments,
    w.fragment_visual_mode,
    case
      when w.fragment_grid_rows is not null
       and w.fragment_grid_cols is not null
       and w.fragment_grid_rows > 0
       and w.fragment_grid_cols > 0
       and w.fragment_grid_rows * w.fragment_grid_cols = w.required_fragments
      then w.fragment_grid_rows
      else factors.rows
    end as rows,
    case
      when w.fragment_grid_rows is not null
       and w.fragment_grid_cols is not null
       and w.fragment_grid_rows > 0
       and w.fragment_grid_cols > 0
       and w.fragment_grid_rows * w.fragment_grid_cols = w.required_fragments
      then w.fragment_grid_cols
      else w.required_fragments / factors.rows
    end as cols
  from wish w
  cross join lateral (
    select max(candidate)::int as rows
    from generate_series(1, floor(sqrt(greatest(w.required_fragments, 1)))::int) as candidate
    where w.required_fragments % candidate = 0
  ) factors
),
mask_payload as (
  select
    n.id,
    jsonb_build_object(
      'version', 1,
      'mode', n.fragment_visual_mode,
      'rows', n.rows,
      'cols', n.cols,
      'total', n.rows * n.cols,
      'revealOrder', (
        select jsonb_agg(index_value order by index_value)
        from generate_series(0, n.rows * n.cols - 1) as index_value
      ),
      'cells', (
        select jsonb_agg(
          jsonb_build_object(
            'index', cell_index,
            'row', cell_index / n.cols,
            'col', cell_index % n.cols,
            'polygon', case
              when n.fragment_visual_mode = 'irregular' then
                case cell_index % 5
                  when 0 then '[{"x":0.02,"y":0.08},{"x":0.88,"y":0.0},{"x":1.0,"y":0.72},{"x":0.18,"y":1.0}]'::jsonb
                  when 1 then '[{"x":0.12,"y":0.0},{"x":1.0,"y":0.14},{"x":0.86,"y":1.0},{"x":0.0,"y":0.84}]'::jsonb
                  when 2 then '[{"x":0.0,"y":0.0},{"x":0.78,"y":0.1},{"x":1.0,"y":1.0},{"x":0.2,"y":0.88}]'::jsonb
                  when 3 then '[{"x":0.18,"y":0.06},{"x":1.0,"y":0.0},{"x":0.82,"y":0.92},{"x":0.0,"y":1.0}]'::jsonb
                  else '[{"x":0.0,"y":0.2},{"x":0.72,"y":0.0},{"x":1.0,"y":0.8},{"x":0.24,"y":1.0}]'::jsonb
                end
              else null
            end
          )
          order by cell_index
        )
        from generate_series(0, n.rows * n.cols - 1) as cell_index
      )
    ) as mask_json,
    jsonb_build_object(
      'version', 1,
      'litIndexes', coalesce((
        select jsonb_agg(index_value order by index_value)
        from generate_series(0, least(greatest(n.earned_fragments, 0), n.rows * n.cols) - 1) as index_value
      ), '[]'::jsonb)
    ) as lit_json
  from normalized n
)
update wish w
set fragment_mask_json = mask_payload.mask_json,
    fragment_lit_json = mask_payload.lit_json
from mask_payload
where w.id = mask_payload.id;
