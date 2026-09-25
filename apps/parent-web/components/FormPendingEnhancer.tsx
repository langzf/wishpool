"use client";

import { useEffect } from "react";

/** Keeps React Server Action forms single-submit safe after hydration. */
export function FormPendingEnhancer() {
  useEffect(() => {
    const isReactActionForm = (form: HTMLFormElement) => {
      const action = form.getAttribute("action") ?? "";
      return action.startsWith("javascript:throw new Error('A React form was unexpectedly submitted") || form.dataset.reactActionForm === "true";
    };
    const mirrorSubmitter = (form: HTMLFormElement, submitter: HTMLButtonElement) => {
      if (!submitter.name || form.querySelector(`input[data-pending-submitter-mirror="true"][name="${CSS.escape(submitter.name)}"]`)) return;
      const mirror = document.createElement("input");
      mirror.name = submitter.name;
      mirror.type = "hidden";
      mirror.value = submitter.value;
      mirror.dataset.pendingSubmitterMirror = "true";
      form.append(mirror);
    };
    const onClick = (event: MouseEvent) => {
      const submitter = event.target instanceof HTMLButtonElement ? event.target : null;
      const form = submitter?.form;
      if (submitter && form && isReactActionForm(form)) mirrorSubmitter(form, submitter);
    };
    const onSubmit = (event: SubmitEvent) => {
      const form = event.target;
      if (!(form instanceof HTMLFormElement) || !isReactActionForm(form)) return;
      const submitter = event.submitter instanceof HTMLButtonElement ? event.submitter : form.querySelector<HTMLButtonElement>('button[type="submit"]');
      if (!submitter) return;
      if (submitter.dataset.pendingLocked === "true") {
        event.preventDefault();
        return;
      }
      mirrorSubmitter(form, submitter);
      submitter.dataset.pendingLocked = "true";
      submitter.setAttribute("aria-busy", "true");
      submitter.setAttribute("aria-disabled", "true");
      submitter.style.pointerEvents = "none";
      submitter.disabled = true;
    };
    document.addEventListener("click", onClick, true);
    document.addEventListener("submit", onSubmit, true);
    return () => {
      document.removeEventListener("click", onClick, true);
      document.removeEventListener("submit", onSubmit, true);
    };
  }, []);
  return null;
}
