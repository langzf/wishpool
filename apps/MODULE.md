# Apps Module

## Purpose

`apps/` contains user-facing clients:

- `apps/mobile` for the Flutter child and parent mobile/tablet app.
- `apps/parent-web` for the parent web workspace.
- `apps/admin-web` for operational and privacy governance tools.

## Current State

All planned clients have been scaffolded:

- `apps/mobile` contains the Flutter source for child tasks, check-in entry, wish progress, room preview, and parent review.
- `apps/parent-web` contains the Next.js parent workspace for review, planning, wish tracking, memories, and local service visibility.
- `apps/admin-web` contains the Next.js operations console for health, queues, privacy requests, storage, workflow links, and audit traces.

They consume shared data from `packages/app-fixtures`, share visual rules from `packages/design-tokens`, and are designed to move to generated clients from `packages/api-contracts`.

## Maintenance Notes

Keep each app's `MODULE.md` current when navigation, data ownership, runtime configuration, API integration, or visual system usage changes.
