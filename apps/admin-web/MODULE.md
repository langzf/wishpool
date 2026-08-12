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
| `components/AdminShell.tsx` | Sidebar navigation and shell layout. |
| `components/AdminDashboard.tsx` | Operations dashboard, queue view, privacy view, and audit table. |
| `lib/runtime.ts` | Runtime links for Admin API, Core API, Temporal UI, and MinIO. |
| `lib/dashboard-data.ts` | Server-side admin dashboard loader with Admin API health probe and sample-data fallback. |
| `app/styles.css` | Token-driven operations UI styling. |

## Verification

Run `npm run check --workspace @wishpool/admin-web`.
