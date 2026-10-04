# WishPool 备份与恢复演练

脚本只对生产 `wishpool` 数据库和 `wishpool-media` 桶执行只读操作。备份只写 D 盘；演练只创建 `wishpool_drill_<时间戳>` 数据库和 `wishpool-media-drill` 桶，不停止或重启容器。

## 备份

在 WSL 中执行：

```bash
cd /mnt/d/Workspaces/wishpool
chmod +x scripts/backup/*.sh
WISHPOOL_BACKUP_ROOT=/mnt/d/wishpool-backups bash scripts/backup/wishpool_backup.sh
```

批次目录包含 `wishpool.dump`、`media/`、`manifest.json`、`SHA256SUMS`、`pg_restore_catalog.txt` 和 `db_counts.tsv`。行数快照来自每张表真实的 `count(*)`，不是 SQL 输出行数。`manifest.json` 复用同一组真实行数。

`mc` 的临时配置写入系统临时目录（`$TMPDIR` 或 `/tmp`），脚本退出时删除；不会在备份目录生成 `mc-config`、`config.json` 或 `config.json.old`。凭据只从环境变量或 `wishpool-core-api` 容器环境读取，不打印、不写入仓库、日志或备份产物。

校验：

```bash
sha256sum -c /mnt/d/wishpool-backups/YYYYMMDD-HHmmss/SHA256SUMS
docker run --rm --pull=never -i postgres:16-alpine pg_restore -l \
  < /mnt/d/wishpool-backups/YYYYMMDD-HHmmss/wishpool.dump
```

## 恢复演练

```bash
bash scripts/backup/wishpool_restore_drill.sh \
  /mnt/d/wishpool-backups/YYYYMMDD-HHmmss
```

默认演练成功或失败退出时都会清理本次创建的演练库和演练桶。需要留给验收检查时使用：

```bash
bash scripts/backup/wishpool_restore_drill.sh \
  /mnt/d/wishpool-backups/YYYYMMDD-HHmmss --keep-drill
```

无人值守执行时可用 `WISHPOOL_DRILL_CONFIRM=CREATE` 明确确认本次创建。

`--cleanup-drill` 使用默认的清理行为。清理数据库使用 `wishpool_app`，只允许删除 `wishpool_drill_*`；清理桶只操作 `wishpool-media-drill`，不会触碰生产桶。

演练会逐表对账、核对演练桶对象数/总字节数，并抽查带浏览器 User-Agent 的 presigned GET。演练凭据通过 `WISHPOOL_DRILL_S3_ACCESS_KEY` / `WISHPOOL_DRILL_S3_SECRET_KEY` 环境变量注入，不能写在命令文件或日志中。

## 生产恢复边界

本目录脚本不执行生产恢复。生产恢复必须经过故障单、DBA/业务双人审批、独立临时库核验和明确回滚点；禁止把脚本改成对 `wishpool` 或 `wishpool-media` 执行写入、删除或覆盖。
