# Parent Web Module

## Purpose

`apps/parent-web` is the parent workspace for daily family operations: review submissions, inspect today's tasks, manage weekly plans, track wishes, open memories, and monitor local realtime/API connectivity.

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
| `app/page.tsx` | Parent workspace entry. |
| `components/Shell.tsx` | Responsive desktop sidebar and mobile bottom navigation. |
| `components/Dashboard.tsx` | Main family dashboard sections. |
| `lib/api.ts` | Runtime Core API and realtime gateway configuration. |
| `lib/dashboard-data.ts` | Server-side dashboard loader with Core API health probe and sample-data fallback. |
| `app/styles.css` | Token-driven responsive UI styles. |

## Verification

Run `npm run check --workspace @wishpool/parent-web`.
