# Admin Web Module

## Purpose

`apps/admin-web` is the local operations and governance console for WishPool. It gives maintainers a dense view of service health, async queues, media and AI processing readiness, privacy requests, object storage, workflow tooling, and audit events through the dedicated Admin API.

## Stack

- Next.js App Router
- React
- TypeScript
- Lucide icons
- Shared `@wishpool/design-tokens`
- Shared `@wishpool/app-fixtures`

## Key Files

| File | Responsibility |
| --- | --- |
| `app/page.tsx` | Admin console entry. |
| `app/actions.ts` | Server actions for local admin-token login, logout, and audited media access grants through Admin API. |
| `components/AuthPanel.tsx` | Admin-token login screen that validates against Admin API before storing the cookie session. |
| `components/AdminShell.tsx` | Sidebar navigation and shell layout. |
| `components/AdminDashboard.tsx` | Operations dashboard, queue view, family metadata, privacy view, media access grant form, and audit table. |
| `lib/runtime.ts` | Runtime links for Admin API, Core API, Temporal UI, MinIO, and admin token. |
| `lib/dashboard-data.ts` | Server-side admin dashboard loader for dashboard counters, families, privacy requests, and audit logs with sample-data fallback. |
| `lib/session.ts` | Cookie-backed Admin Web session containing the local admin token. |
| `app/styles.css` | Token-driven operations UI styling. |

## Runtime Notes

- Operators can enter the local admin token in the Web UI; the app validates it with Admin API and stores it as an httpOnly cookie.
- `WISHPOOL_ADMIN_TOKEN` remains available for scripted local checks.
- `app/actions.ts` posts `/admin/media-access-grants` and redirects back with the short-lived audited access URL for operator use.

## Verification

Run `npm run check --workspace @wishpool/admin-web`.
