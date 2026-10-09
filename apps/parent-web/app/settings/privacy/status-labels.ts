const privacyStatusLabels: Record<string, string> = {
  requested: "已提交",
  verifying: "正在核验",
  locking_family: "正在锁定家庭",
  deleting_records: "正在删除记录",
  deleting_objects: "正在删除文件",
  verifying_deletion: "正在核验删除结果",
  completed: "已完成",
  failed_needs_attention: "需人工处理",
};

export function getPrivacyStatusLabel(status: string): string {
  return privacyStatusLabels[status] ?? "未知状态";
}
