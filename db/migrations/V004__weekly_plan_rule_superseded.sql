alter table weekly_plan_rule
  add column superseded_at timestamptz;

create index idx_weekly_plan_rule_active on weekly_plan_rule(weekly_plan_id, sort_order, created_at)
  where superseded_at is null;
