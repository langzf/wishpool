# 运维事件记录：core-api 服务名别名 `core-api` 丢失（2026-10-07）

## 现象
- `wishpool-workflow-worker` 持续刷 `OutboxPublisher cannot reach core API or claim events; retrying in 30000 ms`
- `outbox_event` 未发布数持续增长（实测 20 → 30 → 37，不再回落）
- E2E `--with-media` 掉到 6/15：`task.today` 45s 内未物化出 todo，`outbox.zero` 失败，`db.evidence` 等 6 项级联失败

## 根因
`wishpool-core-api` 容器创建于 `2026-10-06T02:29:38Z`（为注入 `WISHPOOL_ENABLE_DEBUG_ENDPOINTS` **绕过 compose 重建**了该容器），
重建过程丢失了 compose 才会附加的**服务名网络别名 `core-api`**（容器只剩容器名别名 `wishpool-core-api`）。

底层异常为容器内 DNS 解析失败：

```
Caused by: java.nio.channels.UnresolvedAddressException
  at java.net.http/jdk.internal.net.http.PlainHttpConnection.connectAsync
```

而 compose 中共 **7 个服务**都把 core-api 基址写成服务名：

| 容器 | 相关环境变量 |
|---|---|
| wishpool-workflow-worker | `WISHPOOL_CORE_API_BASE_URL=http://core-api:8080` |
| wishpool-notification-service | 同上 |
| wishpool-media-worker | 同上 |
| wishpool-realtime-gateway | 同上 |
| wishpool-admin-api | 同上 |
| wishpool-parent-web | `NEXT_PUBLIC_WISHPOOL_CORE_API_URL=http://core-api:8080` |
| wishpool-admin-web | 同上 |

## 修复
```bash
# docker network connect --alias 对「已连接」容器会报 endpoint already exists，必须先 disconnect
docker network disconnect wishpool-local_default wishpool-core-api
docker network connect --alias wishpool-core-api --alias core-api wishpool-local_default wishpool-core-api
```
IP 保持 `172.19.0.4` 不变，core-api `/actuator/health` 200。

## 验证（实测）
- worker / parent-web 容器内 `getent hosts core-api` → `172.19.0.4  core-api`
- `outbox_event` 未发布数 **37 → 0** 并连续 100s 稳定为 0（worker 未重启即自愈）
- **E2E `--with-media` → 15/15 PASS（rc=0，33s）**

## 预防
**任何绕过 compose 重建 core-api 的操作，事后必须补回 `core-api` 别名**；或统一改用 `docker compose up -d --no-deps <svc>` 重建以保留别名。
重建后自检一行：`docker exec wishpool-workflow-worker getent hosts core-api`（无输出即已损坏）。
