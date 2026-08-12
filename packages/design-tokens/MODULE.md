# Design Tokens Module

## Purpose

`packages/design-tokens` is the shared visual source for WishPool clients. It keeps color, spacing, radius, typography, shadow, and motion tokens aligned across Flutter, parent web, and admin web.

## Key Files

| File | Responsibility |
| --- | --- |
| `src/index.js` | Token source used by TypeScript clients and exported as plain JavaScript. |
| `src/index.d.ts` | Type declarations for token consumers. |
| `src/check.mjs` | Validates token shape, color format, spacing rhythm, and typography pairing. |

## Verification

Run `npm run check --workspace @wishpool/design-tokens`.
