export const colors = {
  primary: "#2563EB",
  onPrimary: "#FFFFFF",
  secondary: "#059669",
  accent: "#D97706",
  background: "#F8FAFC",
  foreground: "#0F172A",
  muted: "#F1F5FD",
  border: "#E4ECFC",
  destructive: "#DC2626",
  ring: "#2563EB",
  surface: "#FFFFFF",
  surfaceRaised: "#FFF7ED",
  success: "#047857",
  warning: "#B45309",
  info: "#1D4ED8"
};

export const darkColors = {
  primary: "#93C5FD",
  onPrimary: "#0B1220",
  secondary: "#6EE7B7",
  accent: "#FBBF24",
  background: "#0F172A",
  foreground: "#F8FAFC",
  muted: "#1E293B",
  border: "#334155",
  destructive: "#FCA5A5",
  ring: "#93C5FD",
  surface: "#111827",
  surfaceRaised: "#172033",
  success: "#86EFAC",
  warning: "#FCD34D",
  info: "#BFDBFE"
};

export const spacing = {
  xxs: 4,
  xs: 8,
  sm: 12,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
  xxxl: 64
};

export const radius = {
  sm: 6,
  md: 8,
  lg: 12,
  xl: 16,
  pill: 999
};

export const typography = {
  headingFamily: "Fredoka, Nunito, ui-rounded, system-ui, sans-serif",
  bodyFamily: "Nunito, ui-sans-serif, system-ui, sans-serif",
  scale: {
    caption: 12,
    body: 16,
    title: 20,
    section: 24,
    page: 32
  },
  lineHeight: {
    tight: 1.25,
    normal: 1.5,
    relaxed: 1.7
  }
};

export const shadows = {
  raised: "0 10px 24px rgba(15, 23, 42, 0.08)",
  soft: "0 6px 16px rgba(37, 99, 235, 0.12)",
  inset: "inset 0 -2px 0 rgba(15, 23, 42, 0.08)"
};

export const motion = {
  fast: "150ms ease-out",
  normal: "220ms ease-out",
  slow: "320ms ease-out"
};

export const cssVariables = {
  "--color-primary": colors.primary,
  "--color-on-primary": colors.onPrimary,
  "--color-secondary": colors.secondary,
  "--color-accent": colors.accent,
  "--color-background": colors.background,
  "--color-foreground": colors.foreground,
  "--color-muted": colors.muted,
  "--color-border": colors.border,
  "--color-destructive": colors.destructive,
  "--color-ring": colors.ring,
  "--color-surface": colors.surface,
  "--color-surface-raised": colors.surfaceRaised,
  "--font-heading": typography.headingFamily,
  "--font-body": typography.bodyFamily,
  "--shadow-raised": shadows.raised,
  "--shadow-soft": shadows.soft,
  "--radius-sm": `${radius.sm}px`,
  "--radius-md": `${radius.md}px`,
  "--radius-lg": `${radius.lg}px`,
  "--radius-xl": `${radius.xl}px`
};

export const flutterTokens = {
  colors,
  darkColors,
  spacing,
  radius,
  typography
};
