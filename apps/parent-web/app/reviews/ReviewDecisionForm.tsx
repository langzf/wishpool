"use client";

import { useState } from "react";
import { approveReviewAction, requestRevisionAction } from "@/app/actions";
import { useFormStatus } from "react-dom";

type Props = {
  submissionId: string;
  returnTo: string;
  feedbackText?: string;
};

export function ReviewDecisionForm({ submissionId, returnTo, feedbackText = "" }: Props) {
  const [submitting, setSubmitting] = useState(false);

  return (
    <div className="panel" style={{ display: "grid", gap: 16 }}>
      <div>
        <h2 style={{ margin: 0 }}>填写审核反馈</h2>
        <p className="muted" style={{ marginBottom: 0 }}>反馈会发送给孩子，提交前可以按需修改。</p>
      </div>
      <form
        action={requestRevisionAction}
        onSubmit={() => setSubmitting(true)}
        style={{ display: "grid", gap: 12 }}
      >
        <input name="returnTo" type="hidden" value={returnTo} />
        <input name="preserveFeedback" type="hidden" value="true" />
        <input name="submissionId" type="hidden" value={submissionId} />
        <label>
          <span className="muted">退回反馈</span>
          <textarea name="feedbackText" defaultValue={feedbackText} rows={4} required placeholder="请告诉孩子还可以怎样补充。" style={{ width: "100%", marginTop: 6 }} />
        </label>
        <SubmitButton className="secondary-button" disabled={submitting}>退回</SubmitButton>
      </form>
      <form
        action={approveReviewAction}
        onSubmit={() => setSubmitting(true)}
        style={{ display: "grid", gap: 12 }}
      >
        <input name="returnTo" type="hidden" value={returnTo} />
        <input name="preserveFeedback" type="hidden" value="true" />
        <input name="submissionId" type="hidden" value={submissionId} />
        <label>
          <span className="muted">通过反馈</span>
          <textarea name="feedbackText" defaultValue={feedbackText} rows={4} required placeholder="写下对孩子这次完成情况的鼓励。" style={{ width: "100%", marginTop: 6 }} />
        </label>
        <SubmitButton className="primary-button" disabled={submitting}>通过</SubmitButton>
      </form>
    </div>
  );
}

function SubmitButton({ className, disabled, children }: Readonly<{ className: string; disabled: boolean; children: React.ReactNode }>) {
  const status = useFormStatus();
  const pending = disabled || status.pending;
  return <button className={className} type="submit" disabled={pending}>{pending ? "提交中…" : children}</button>;
}
