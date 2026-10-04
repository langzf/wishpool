# WishPool T22b E2E

在仓库根目录执行：

```powershell
python scripts/e2e/wishpool_e2e.py > out.txt 2>&1; echo RC=$?
```

默认运行使用本地 core-api `18080`、家长端 `18300` 和 realtime `18081`，每次生成新的 `runId`、`e2e-w-<runId>`、计划规则、任务和 submission，不复用历史计划，也不删除历史数据。保存计划后会轮询当天任务（默认 45 秒）。

默认包含媒体链路：`upload-sessions -> presigned PUT -> finalize -> submission`。presigned PUT 显式使用浏览器 User-Agent；可用 `--user-agent` 覆盖。媒体最终状态允许 `ready` 或 `processing`，submission 的 `mediaAssetIds` 必须非空。

脚本共 15 项检查，成功打印 `SUMMARY 15/15 PASS` 并返回 0；任一检查失败返回 1。不要用管道判断退出码，使用上面的重定向写法。

常用验收命令：

```powershell
python scripts/e2e/wishpool_e2e.py
python scripts/e2e/wishpool_e2e.py --with-media
python scripts/e2e/wishpool_e2e.py --inject ai.precheck
python scripts/e2e/wishpool_e2e.py --core-api-url http://localhost:18089
```

输出末尾会打印本次新增对象（table + id）、`e2e-` 数据前缀和 `scripts/e2e/last_result.json`。数据库只读证据包含 submission、ai_job、ai_precheck、model_invocation_log、`submission.ai_prechecked` outbox/family event 关联和 reward_ledger 增长。
