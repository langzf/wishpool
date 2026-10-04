alter table ai_job
  drop constraint ai_job_status_check;

alter table ai_job
  add constraint ai_job_status_check
  check (status in ('queued', 'media_preparing', 'running', 'succeeded', 'failed', 'failed_retryable', 'failed_final', 'cancelled'));
