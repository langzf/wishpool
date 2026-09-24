# WishPool Core-API 接口可用性报告

- 日期: 2026-08-18
- 范围: services/core-api 全部公开/内部接口（19 Controller, 60+ endpoints）
- 方式: 真实调用运行中服务（http://localhost:8080），纯 Python 标准库脚本
- 结果: **168/168 测试点全部通过（17 个测试文件）**

## 各模块测试结果

| 模块 | 测试文件 | 结果 |
|---|---|---|
| Admin | test_admin.py | 5/5 |
| Auth | test_auth.py | 12/12 |
| Child | test_child.py | 9/9 |
| Family | test_family.py | 17/17 |
| Home/Dashboard | test_home.py | 10/10 |
| Media | test_media.py | 7/7 |
| Memories | test_memories.py | 11/11 |
| Notifications | test_notifications.py | 11/11 |
| Pairing | test_pairing.py | 8/8 |
| Plans/TaskPlanning | test_plans.py | 14/14 |
| Privacy | test_privacy.py | 6/6 |
| Reviews | test_reviews.py | 15/15 |
| Room | test_room.py | 7/7 |
| Submissions | test_submissions.py | 8/8 |
| Sync | test_sync.py | 4/4 |
| System | test_system.py | 2/2 |
| Wishes | test_wishes.py | 22/22 |
| **合计** | **17 文件** | **168/168** |

每个接口覆盖: 正常流程 2xx / 参数校验 400 / 未认证 401 / 不存在 404。

## 测试过程中发现并修复的问题

1. parent-dashboard 500: MemoryService SQL 中 before_start 参数传 null 导致 Postgres 无法推断类型 -> cast(:before_start as date)
2. 6 处 limit 动态参数类型风险 (Admin/Memory/Notification/Outbox/Media/Sync) -> cast(... as integer)
3. postponeTask 中 coalesce(:reason, ...) 可空参数 -> cast(:reason as text)
4. 缺必填参数返回 500（MissingServletRequestParameterException 未映射）-> ApiExceptionHandler 映射 400（含 BindException）
5. 畸形 UUID/日期绑定返回 500 -> ApiExceptionHandler 映射 400
6. parent-web server action 吞掉 redirect 错误（页面显示 NEXT_REDIRECT）-> isRedirectError 判断重新抛出
7. workflow-worker 无法反序列化 Kotlin workflow 参数（Temporal 队列积压全挂）-> 加 jackson-module-kotlin + 自定义 DataConverter
8. force-recreate 后 Gradle 下载 10s 超时导致 Java 服务起不来 -> gradle-wrapper networkTimeout=60000, retries=2

## 测试方法

cd D:/Workspaces/wishpool && python3 tests/api/test_<module>.py   # 输出 PASS/FAIL + TOTAL

共享基座: tests/api/conftest.py（api() helper + 登录/家庭/孩子准备函数）
验证码: 调试模式固定 123456（WISHPOOL_LOCAL_DEBUG_PHONE_CODE=true）

## 遗留建议

- ai-worker / media-worker 为被动 HTTP 服务（无请求无日志属正常），provider=deterministic（本地无外部 LLM 依赖）
- 建议将测试纳入 CI，每次部署后自动回归