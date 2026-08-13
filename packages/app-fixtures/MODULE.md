# App Fixtures Module

## Purpose

`packages/app-fixtures` provides coherent sample data for clients, design previews, and service tests. Fixtures cover the main WishPool loop: family, child, scheduled today tasks, submissions under review, wishes, weekly plan rules, memories, room items, inbox notifications, notification preferences, privacy requests, family metadata, and dashboard metrics.

## Key Files

| File | Responsibility |
| --- | --- |
| `src/index.js` | Shared data objects and `getFixtureSnapshot()`. |
| `src/index.d.ts` | Typed declarations for TypeScript consumers, Web loaders, and mobile data mapping. |
| `src/check.mjs` | Consistency checks for linked fixture data. |

## Verification

Run `npm run check --workspace @wishpool/app-fixtures`.
