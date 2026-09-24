alter table wish
  drop constraint if exists wish_fragment_grid_rows_check,
  drop constraint if exists wish_fragment_grid_cols_check;

alter table wish
  add constraint wish_fragment_grid_rows_check
    check (fragment_grid_rows is null or fragment_grid_rows > 0),
  add constraint wish_fragment_grid_cols_check
    check (fragment_grid_cols is null or fragment_grid_cols > 0);
