# rippl-dashboard CI Redesign

Align rippl-dashboard frontend with rippl-web color tokens, typography, and visual identity. Add dark mode.

## Decisions

- **Scope**: Full CI alignment — colors, typography, border radii, shadows, warm paper background
- **Dark mode**: Yes, with warm dark palette derived from ink hue (28-32deg)
- **Chart colors**: Warm-shift activity pie chart. Domain-specific colors (Claude, ChatGPT, etc.) unchanged
- **Theme system**: CSS variables + `@theme inline` Tailwind exposure. No shadcn/ui (dashboard doesn't use it)
- **Fonts**: Inter (body) + Source Serif 4 (logo only). Loaded via Google Fonts `<link>` in `index.html`

## Token Architecture

Three layers in `frontend/src/index.css`:

### Layer 1 — Brand Tokens

Copied from rippl-web `globals.css`. Never referenced directly in components.

```css
:root {
  --moss: #5C7A52;
  --moss-dark: #3F5639;
  --moss-tint: #E5EBD5;
  --terra: #B05F3F;
  --terra-dark: #8B4830;
  --terra-tint: #F2DFD2;
  --paper: #EFEAE0;
  --paper-2: #E8E2D4;
  --cream: #FBF8F1;
  --surface: #FFFFFF;
  --ink: #1F1C16;
  --ink-2: #56524A;
  --ink-3: #8C8478;
  --line: #D8CFB9;
  --line-2: #E5DDC9;
  --destructive: #b3261e;
}
```

### Layer 2 — Semantic Tokens (Light Mode)

What components actually use via Tailwind utilities.

```css
:root {
  --bg-page: var(--paper);
  --bg-card: var(--surface);
  --bg-nav: var(--cream);
  --bg-input: var(--surface);
  --bg-muted: var(--line-2);
  --bg-accent: var(--moss-tint);
  --bg-error: #fef2f2;
  --bg-success: #f0fdf4;

  --fg: var(--ink);
  --fg-secondary: var(--ink-2);
  --fg-muted: #776D60;
  --fg-on-primary: var(--cream);
  --fg-error: var(--destructive);
  --fg-success: #15803d;
  --fg-active: var(--moss-dark);

  --border-default: var(--line);
  --border-subtle: var(--line-2);

  --primary: var(--moss-dark);
  --primary-hover: var(--moss);
  --secondary: var(--terra);
  --ring: var(--moss);

  --font-sans: 'Inter', system-ui, sans-serif;
  --font-serif: 'Source Serif 4', Georgia, serif;

  --radius-card: 8px;
  --radius-input: 8px;
  --radius-pill: 999px;
}
```

### Layer 3 — Dark Mode

Toggled via `.dark` class on `<html>`. Warm dark browns, not cold grays.

```css
.dark {
  --bg-page: #1a1814;
  --bg-card: #252118;
  --bg-nav: #1f1c16;
  --bg-input: #2a2620;
  --bg-muted: #3a3530;
  --bg-accent: #2d3a28;
  --bg-error: #3b1c1c;
  --bg-success: #1a2e1a;

  --fg: var(--cream);
  --fg-secondary: #b8b0a0;
  --fg-muted: #9a9185;
  --fg-on-primary: var(--cream);
  --fg-error: #f87171;
  --fg-success: #86efac;
  --fg-active: var(--moss-tint);

  --border-default: #3a3530;
  --border-subtle: #2a2620;

  --primary: var(--moss);
  --primary-hover: var(--moss-tint);
  --ring: var(--moss);
}
```

### Tailwind Exposure

Semantic tokens exposed via namespaced `@theme inline` — each CSS variable registers under the correct Tailwind namespace to generate precise utilities with no waste:

```css
@theme inline {
  /* bg-* utilities */
  --background-page: var(--bg-page);
  --background-card: var(--bg-card);
  --background-nav: var(--bg-nav);
  --background-input: var(--bg-input);
  --background-muted: var(--bg-muted);
  --background-accent: var(--bg-accent);
  --background-error: var(--bg-error);
  --background-success: var(--bg-success);

  /* text-* utilities */
  --text-fg: var(--fg);
  --text-fg-secondary: var(--fg-secondary);
  --text-fg-muted: var(--fg-muted);
  --text-fg-on-primary: var(--fg-on-primary);
  --text-fg-error: var(--fg-error);
  --text-fg-success: var(--fg-success);
  --text-fg-active: var(--fg-active);

  /* General colors (bg-primary, text-primary, border-primary all work) */
  --color-primary: var(--primary);
  --color-primary-hover: var(--primary-hover);
  --color-secondary: var(--secondary);
  --color-destructive: var(--destructive);
  --color-ring: var(--ring);

  /* border-* utilities */
  --border-color-default: var(--border-default);
  --border-color-subtle: var(--border-subtle);

  /* radius utilities */
  --radius-card: var(--radius-card);
  --radius-input: var(--radius-input);
  --radius-pill: var(--radius-pill);

  /* font utilities */
  --font-sans: var(--font-sans);
  --font-serif: var(--font-serif);
}
```

Generates: `bg-page`, `bg-card`, `text-fg`, `text-fg-muted`, `bg-primary`, `text-primary`, `border-default`, `rounded-card`, `font-serif`, etc.

## Component Color Mapping

| Current Class | New Class | Files |
|---|---|---|
| `bg-gray-50` | `bg-page` | Layout, Login |
| `bg-white` | `bg-card` | All cards, chart containers |
| `bg-white shadow-sm` (nav) | `bg-nav border-b border-default` | Layout |
| `text-gray-900` | `text-fg` | Headings, strong text |
| `text-gray-600`, `text-gray-700` | `text-fg-secondary` | Nav links, body |
| `text-gray-500` | `text-fg-muted` | Labels, descriptions |
| `text-gray-400` | `text-fg-muted` | Hints, sign out |
| `bg-indigo-600` | `bg-primary` | Primary buttons |
| `hover:bg-indigo-700` | `hover:bg-primary-hover` | Button hover |
| `text-indigo-600` | `text-fg-active` | Active nav, links |
| `bg-indigo-100` | `bg-accent` | Step indicators |
| `border-gray-300` | `border-default` | Input borders |
| `hover:border-indigo-400` | `hover:border-ring` | Input hover |
| `ring-indigo-500` | `ring-ring` | Focus rings |
| `bg-red-600` | `bg-destructive` | Delete button |
| `bg-red-50` | `bg-error` | Error backgrounds |
| `text-red-600`, `text-red-700` | `text-fg-error` | Error text |
| `bg-green-50` | `bg-success` | Success background |
| `text-green-700` | `text-fg-success` | Success text |
| `bg-gray-200` | `bg-muted` | Secondary buttons, toggles |
| `shadow` | `shadow-sm` | Cards (softer) |

## Typography

- `font-sans` (`Inter`) on `<body>` — default for everything
- `font-serif` (`Source Serif 4`) on logo "rippl" only
- Loaded via Google Fonts `<link>` in `frontend/index.html`
- No monospace needed (no code blocks in dashboard)

## Dark Mode Toggle

- `ThemeProvider` React context: reads `localStorage('theme')`, falls back to `prefers-color-scheme`
- Toggles `.dark` class on `document.documentElement`
- Sun/moon toggle button in nav, next to "Sign out"
- Flash prevention: inline `<script>` in `index.html` `<head>` reads preference before React loads

## Chart Color Warm-Shift

Activity pie chart palette (Trends.tsx):

| Current | New | Token |
|---|---|---|
| `#6366F1` (indigo) | `#5C7A52` | moss |
| `#10B981` (emerald) | `#B05F3F` | terra |
| `#F59E0B` (amber) | `#8B4830` | terra-dark |
| `#EF4444` (red) | `#1F4E68` | ocean-fg |

Domain colors in `domains.ts` unchanged — they represent external brand identities.

## Known Limitations

- **Recharts dark mode**: Chart axes, tooltips, and grid lines won't adapt to dark mode. Recharts renders its own SVG with hardcoded styles. Fixing requires passing theme-aware props to each chart component — deferred to a follow-up.

## Files Changed

1. `frontend/index.html` — Google Fonts links + theme flash prevention script
2. `frontend/src/index.css` — Full theme system (brand tokens, semantic tokens, dark mode, `@theme inline`)
3. `frontend/src/context/ThemeContext.tsx` — New: ThemeProvider + useTheme hook
4. `frontend/src/components/Layout.tsx` — Nav restyled, dark mode toggle, font-serif on logo
5. `frontend/src/pages/Login.tsx` — Color class swap
6. `frontend/src/pages/Dashboard.tsx` — Color class swap
7. `frontend/src/pages/Trends.tsx` — Color class swap + pie chart palette
8. `frontend/src/pages/Mirror.tsx` — Color class swap
9. `frontend/src/pages/Settings.tsx` — Color class swap
10. `frontend/src/components/TimeSavedCard.tsx` — Color class swap
11. `frontend/src/components/TrendChart.tsx` — Color class swap
12. `frontend/src/components/MirrorMomentCard.tsx` — Color class swap
13. `frontend/src/components/CollectorCard.tsx` — Color class swap
14. `frontend/src/components/OnboardingChecklist.tsx` — Color class swap + accent colors
15. `frontend/src/App.tsx` — Wrap with ThemeProvider
