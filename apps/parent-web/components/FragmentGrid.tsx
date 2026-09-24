import { Lock } from "lucide-react";
import type { CSSProperties } from "react";

type FragmentStatusMeta = {
  label: string;
  className: string;
};

export type FragmentPoint = {
  x: number;
  y: number;
};

export type FragmentMaskCell = {
  index: number;
  row: number;
  col: number;
  polygon?: FragmentPoint[] | null;
};

export type FragmentMask = {
  version: number;
  mode: string;
  rows: number;
  cols: number;
  total: number;
  revealOrder: number[];
  cells: FragmentMaskCell[];
};

type FragmentGridProps = Readonly<{
  current: number;
  target: number;
  imageUrl?: string;
  mode?: string;
  rows?: number;
  cols?: number;
  mask?: FragmentMask;
  litIndexes?: number[];
  title?: string;
  description?: string;
  statusMeta?: FragmentStatusMeta;
  className?: string;
}>;

const supportedModes = new Set(["grid_reveal", "puzzle_lines", "irregular"]);

export function FragmentGrid({
  current,
  target,
  imageUrl,
  mode = "grid_reveal",
  rows,
  cols,
  mask,
  litIndexes,
  title,
  description,
  statusMeta,
  className
}: FragmentGridProps) {
  const layout = fragmentLayout(target, rows, cols, mask);
  const cells = layout.rows * layout.cols;
  const fallbackFilled = Math.min(cells, Math.max(current, 0));
  const normalizedLitIndexes = normalizeLitIndexes(litIndexes, cells);
  const litSet = normalizedLitIndexes ? new Set(normalizedLitIndexes) : null;
  const filled = litSet?.size ?? fallbackFilled;
  const complete = filled >= cells && target > 0;
  const visualMode = typeof layout.mode === "string" && supportedModes.has(layout.mode) ? layout.mode : supportedModes.has(mode) ? mode : "grid_reveal";
  const style = {
    "--fragment-rows": layout.rows,
    "--fragment-cols": layout.cols
  } as CSSProperties;
  const cellGridStyle: CSSProperties = {
    display: "grid",
    gridTemplateColumns: `repeat(${layout.cols}, minmax(0, 1fr))`,
    gridTemplateRows: `repeat(${layout.rows}, minmax(0, 1fr))`
  };
  const variant = placeholderVariant(title ?? "");
  return (
    <div
      className={`fragment-grid fragment-mode-${visualMode} ${complete ? "fragment-complete" : ""} ${className ?? ""}`}
      style={style}
      aria-label={`心愿碎片 ${current} / ${target}`}
    >
      <div
        className={`fragment-card-backdrop ${imageUrl ? "has-image" : `placeholder-${variant}`}`}
        style={imageUrl ? { backgroundImage: `url(${imageUrl})` } : undefined}
      />
      <div className="fragment-cells" style={cellGridStyle} aria-hidden="true">
        {layout.cells.map((cell) => (
          <span
            className={(litSet ? litSet.has(cell.index) : cell.index < fallbackFilled) ? "fragment-cell filled" : "fragment-cell"}
            key={cell.index}
            style={fragmentCellStyle(cell, visualMode)}
          >
            <span className="fragment-lock">
              <Lock size={14} aria-hidden="true" />
            </span>
          </span>
        ))}
      </div>
      {title ? (
        <div className="fragment-card-caption">
          {statusMeta ? <span className={`status-pill ${statusMeta.className}`}>{statusMeta.label}</span> : null}
          <h3>{title}</h3>
          {description ? <p>{description}</p> : null}
        </div>
      ) : null}
      <strong className="fragment-progress-badge">
        <span>已收集</span>
        {current} / {target}
      </strong>
    </div>
  );
}

function fragmentLayout(target: number, rows?: number, cols?: number, mask?: FragmentMask) {
  if (isValidMask(mask)) {
    return {
      rows: mask.rows,
      cols: mask.cols,
      mode: mask.mode,
      cells: mask.cells
    };
  }
  const normalizedTarget = isPositiveInteger(target) ? target : 1;
  if (isPositiveInteger(rows) && isPositiveInteger(cols) && rows * cols === normalizedTarget) {
    return { rows, cols, mode: undefined, cells: defaultCells(rows, cols) };
  }
  let bestRows = 1;
  let bestCols = normalizedTarget;
  for (let candidateRows = 1; candidateRows <= Math.sqrt(normalizedTarget); candidateRows += 1) {
    if (normalizedTarget % candidateRows !== 0) continue;
    bestRows = candidateRows;
    bestCols = normalizedTarget / candidateRows;
  }
  return { rows: bestRows, cols: bestCols, mode: undefined, cells: defaultCells(bestRows, bestCols) };
}

function isPositiveInteger(value: number | undefined): value is number {
  return typeof value === "number" && Number.isInteger(value) && value > 0;
}

function fragmentCellStyle(cell: FragmentMaskCell, mode: string): CSSProperties {
  const polygon = polygonClipPath(cell.polygon) ?? irregularPolygon(cell.index);
  return {
    animationDelay: `${Math.min(cell.index * 22, 220)}ms`,
    clipPath: mode === "irregular" ? `polygon(${polygon})` : undefined
  };
}

function isValidMask(mask: FragmentMask | undefined): mask is FragmentMask {
  if (!mask || !isPositiveInteger(mask.rows) || !isPositiveInteger(mask.cols) || !isPositiveInteger(mask.total)) return false;
  if (mask.rows * mask.cols !== mask.total || mask.cells.length !== mask.total) return false;
  return supportedModes.has(mask.mode) && mask.cells.every((cell) => Number.isInteger(cell.index) && cell.index >= 0 && cell.index < mask.total);
}

function defaultCells(rows: number, cols: number): FragmentMaskCell[] {
  return Array.from({ length: rows * cols }).map((_, index) => ({
    index,
    row: Math.floor(index / cols),
    col: index % cols
  }));
}

function normalizeLitIndexes(litIndexes: number[] | undefined, total: number) {
  if (!litIndexes || litIndexes.length === 0) return null;
  const normalized = Array.from(new Set(litIndexes.filter((index) => Number.isInteger(index) && index >= 0 && index < total)));
  return normalized.length > 0 ? normalized : null;
}

function polygonClipPath(polygon: FragmentPoint[] | null | undefined) {
  if (!polygon || polygon.length < 3) return null;
  return polygon.map((point) => `${point.x * 100}% ${point.y * 100}%`).join(", ");
}

function placeholderVariant(title: string) {
  return title.charCodeAt(0) % 2 === 0 ? "spark" : "garden";
}

function irregularPolygon(index: number) {
  const shapes = [
    "0 8%, 88% 0, 100% 72%, 18% 100%",
    "12% 0, 100% 14%, 86% 100%, 0 84%",
    "0 0, 78% 10%, 100% 100%, 20% 88%",
    "18% 6%, 100% 0, 82% 92%, 0 100%",
    "0 20%, 72% 0, 100% 80%, 24% 100%"
  ];
  return shapes[index % shapes.length];
}
