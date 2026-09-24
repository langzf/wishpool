# WishPool NAS-backed development

The NAS provides one shared PostgreSQL, Redis, and MinIO deployment for multiple
applications. WishPool owns logical resources inside those services rather than
deploying duplicate infrastructure containers.

## Resource boundaries

| Shared service | WishPool resource |
| --- | --- |
| PostgreSQL | Database `wishpool`, login role `wishpool_app` |
| Redis | Logical database `1`; every key must retain the `wishpool:` prefix |
| MinIO | Private bucket `wishpool-media` |
| Cloudflare Tunnel | Public S3 endpoint `https://minio.yueying.cloud` |

Temporal is WishPool-specific. Its server runs on the NAS, while its two
databases live in the shared PostgreSQL instance.

## Configuration

Copy `.env.example` to an ignored `.env.nas`. Retrieve generated database
credentials from `/home/18457113512/wishpool-infra/.env` on the NAS. Do not
commit either the database password or WishPool's bucket-scoped MinIO credentials.

The stable LAN name is `Z4Pro-69HO.local`. The current DHCP address must not be
hard-coded; reserve the NAS address in the router before unattended operation.

Core API must use the LAN endpoint for S3 management operations and the public
endpoint for browser/mobile presigned URLs:

```text
WISHPOOL_S3_ENDPOINT=http://Z4Pro-69HO.local:9000
WISHPOOL_S3_PUBLIC_ENDPOINT=https://minio.yueying.cloud
```

## Cloudflare cutover

Keep the existing public MinIO origin active until the NAS bucket is populated
and presigned PUT/GET checks pass. Then update the named tunnel route for
`minio.yueying.cloud` to the NAS MinIO API (`http://127.0.0.1:9000` when the
connector runs on the NAS). Never expose the MinIO console port.

## Isolation note

Redis logical databases are operational separation, not a security boundary.
The existing shared Redis currently permits the default unauthenticated user.
Introduce persisted Redis ACL users in a coordinated maintenance window because
enabling authentication also requires updating every existing consumer.
