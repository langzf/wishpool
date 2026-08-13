#!/usr/bin/env sh
set -eu

app_dir="$1"
app_name="$2"

required_files="
${app_dir}/package.json
${app_dir}/next.config.mjs
${app_dir}/tsconfig.json
${app_dir}/app/layout.tsx
${app_dir}/app/page.tsx
${app_dir}/app/styles.css
${app_dir}/MODULE.md
"

for file in $required_files; do
  test -f "$file"
done

node -e "
const fs = require('node:fs');
const pkg = JSON.parse(fs.readFileSync('${app_dir}/package.json', 'utf8'));
for (const dep of ['next', 'react', 'react-dom', '@wishpool/design-tokens', '@wishpool/app-fixtures']) {
  if (!pkg.dependencies || !pkg.dependencies[dep]) throw new Error('${app_name} missing dependency ' + dep);
}
"

rg --glob '!node_modules/**' --glob '!.next/**' -n "export default function|aria-label|Navigation|lucide-react|@wishpool/app-fixtures" "$app_dir" >/dev/null
if [ -f "${app_dir}/lib/dashboard-data.ts" ]; then
  rg -n "load.*DashboardData|source: \"api\"|source: \"admin-api\"|source: \"fixture\"" "${app_dir}/lib/dashboard-data.ts" >/dev/null
else
  echo "${app_name} missing dashboard data loader" >&2
  exit 1
fi
blocked_pattern='TO''DO|MV''P|MP''V|P''0|P''1|终''态|阶''段'
rg --glob '!node_modules/**' --glob '!.next/**' -n "$blocked_pattern" "$app_dir" && exit 1 || true

echo "validated ${app_name} source structure"
