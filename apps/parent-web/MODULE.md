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
| `app/api/realtime/route.ts` | Same-origin SSE proxy from cookie session to Realtime Gateway. |
| `app/actions.ts` | Server actions for phone login, parent context selection, setup, review decisions, skip/postpone, weekly plan save, pairing code creation, wish creation, memory export, and room arrangement. |
| `components/AuthPanel.tsx` | Phone-login and family/child setup screens before the workspace renders family data. |
| `components/Shell.tsx` | Responsive desktop sidebar and mobile bottom navigation. |
| `components/Dashboard.tsx` | Main family dashboard sections, empty states, notification inbox, preference controls, and command forms. |
| `components/RealtimeRefresh.tsx` | Client-side SSE listener that persists the cursor and refreshes server-rendered data after family events. |
| `lib/api.ts` | Runtime Core API, realtime gateway, parent access token, family id, and child id configuration. |
| `lib/core-client.ts` | Server-side JSON helpers for Core API reads and commands. |
| `lib/dashboard-data.ts` | Server-side dashboard loader for `/families/{familyId}/parent-dashboard` with sample-data fallback. |
| `lib/profile-data.ts` | Loads `/me` plus child profiles for family/child selection. |
| `lib/session.ts` | Cookie-backed parent web session containing tokens and selected family/child ids. |
| `app/styles.css` | Token-driven responsive UI styles. |

## Runtime Notes

- Parents can log in with a phone code from the Web UI; the app stores a cookie session and selected family/child ids.
- Realtime updates flow through `/api/realtime` so the browser never needs to read the bearer token.
- `WISHPOOL_PARENT_ACCESS_TOKEN`, `WISHPOOL_PARENT_FAMILY_ID`, and `WISHPOOL_PARENT_CHILD_ID` remain available for scripted local checks.
- Review, task adjustment, weekly plan, pairing, wish, memory export, room arrangement, inbox read state, and notification preference forms use server actions with bearer auth and idempotency keys where the Core API command supports them.
- Empty results from the real Core API render as explicit empty states instead of falling back to shared fixtures.
- The dashboard still renders coherent shared fixtures when local API credentials are absent.

## Verification

Run `npm run check --workspace @wishpool/parent-web`.
