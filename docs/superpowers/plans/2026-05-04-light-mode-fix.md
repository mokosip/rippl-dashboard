# Light Mode Token Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make all pond design components respect light/dark mode by replacing hardcoded hex colors with Tailwind CSS token classes. Light mode should use the existing rippl-web warm palette; dark mode keeps the pond forest green palette.

**Architecture:** The CSS token system already has both light (`:root`) and dark (`.dark`) variants. The problem is that pond components bypass tokens with inline `style={{ color: '#5C7A52' }}` etc. Fix: replace every inline color with the matching Tailwind class (`text-fg-muted`, `text-fg`, `bg-card`, etc.). For cases where no matching token exists (e.g. accent green for stat numbers), add new semantic tokens to both light and dark blocks. Canvas components need a different approach — they read colors from CSS custom properties at render time.

**Tech Stack:** Tailwind CSS v4, CSS custom properties, Canvas 2D API

---

## New Semantic Tokens Needed

The pond design introduced visual roles that don't map to existing tokens. Add these to both `:root` and `.dark`:

| Token | Light value | Dark value | Usage |
|-------|------------|------------|-------|
| `--fg-accent` | `var(--moss-dark)` (#3F5639) | `#8fb87a` | Hero stat number, highlight text, data point labels |
| `--bg-nav-blur` | `rgba(251,248,241,0.85)` | `rgba(15,26,15,0.85)` | Nav backdrop |
| `--bg-tooltip` | `rgba(255,255,255,0.95)` | `rgba(15,26,15,0.9)` | Chart tooltip background |
| `--border-tooltip` | `var(--line)` | `rgba(92,122,82,0.3)` | Chart tooltip border |
| `--chart-grid` | `rgba(31,28,22,0.06)` | `rgba(92,122,82,0.08)` | Chart grid lines |
| `--chart-wave-1` | `rgba(63,86,57,0.9)` | `rgba(143,184,122,0.9)` | Primary wave stroke |
| `--chart-fill-1` | `rgba(63,86,57,0.08)` | `rgba(92,122,82,0.12)` | Primary wave fill |
| `--chart-wave-2` | `rgba(176,95,63,0.7)` | `rgba(176,95,63,0.7)` | Secondary wave stroke |
| `--chart-fill-2` | `rgba(176,95,63,0.06)` | `rgba(176,95,63,0.08)` | Secondary wave fill |
| `--chart-ripple` | `rgba(63,86,57,0.4)` | `rgba(143,184,122,0.4)` | Chart hover ripple |
| `--chart-dot` | `var(--moss-dark)` | `rgba(143,184,122,0.9)` | Chart data point |
| `--click-ripple` | `rgba(92,122,82,0.3)` | `rgba(92,122,82,0.5)` | Global click ripple |
| `--sync-dot` | `var(--moss)` | `#5C7A52` | Sync indicator dot |
| `--sync-pulse` | `rgba(92,122,82,0.3)` | `rgba(92,122,82,0.4)` | Sync indicator ring |
| `--heatmap-fill` | `rgba(63,86,57,VAR)` | `rgba(92,122,82,VAR)` | Heatmap cell (alpha varies) |
| `--heatmap-glow` | `rgba(63,86,57,VAR)` | `rgba(143,184,122,VAR)` | Heatmap cell glow |
| `--ambient-ripple` | `rgba(92,122,82,VAR)` | `rgba(92,122,82,VAR)` | Ambient background ripple |

---

## Hardcoded Color → Token Mapping

Across all components, these hex values map to these tokens:

| Hardcoded | Semantic role | Replace with |
|-----------|--------------|-------------|
| `#c8dfc0` | Primary text | `text-fg` class |
| `#8fb87a` | Accent/highlight | `text-fg-accent` class (new) |
| `#6a9a5a` | Secondary text | `text-fg-secondary` class |
| `#5C7A52` | Muted/label text | `text-fg-muted` class |
| `#f87171` | Error/destructive | `text-fg-error` class |
| `#0f1a0f` | Text on primary | `text-fg-on-primary` class |
| `rgba(92,122,82,0.06)` | Card background | `bg-card` class |
| `rgba(92,122,82,0.15)` | Card border | `border-default` class |
| `rgba(92,122,82,0.1)` | Input background | `bg-input` class |
| `rgba(92,122,82,0.2)` | Input border | handled by token |
| `rgba(15,26,15,0.85)` | Nav background | `var(--bg-nav-blur)` |
| `rgba(92,122,82,0.08)` | Muted background | `bg-muted` class |
| `rgba(92,122,82,0.15)` | Accent background | `bg-accent` class |

---

### Task 1: Add new CSS tokens

**Files:**
- Modify: `frontend/src/index.css`

- [ ] **Step 1: Add new tokens to the light `:root` block** (second `:root`, the semantic one)

After `--fg-active: var(--moss-dark);` add:

```css
  --fg-accent: var(--moss-dark);

  --bg-nav-blur: rgba(251,248,241,0.85);
  --bg-tooltip: rgba(255,255,255,0.95);
  --border-tooltip: var(--line);

  --chart-grid: rgba(31,28,22,0.06);
  --chart-wave-1: rgba(63,86,57,0.9);
  --chart-fill-1: rgba(63,86,57,0.08);
  --chart-wave-2: rgba(176,95,63,0.7);
  --chart-fill-2: rgba(176,95,63,0.06);
  --chart-wave-3: rgba(31,78,104,0.7);
  --chart-fill-3: rgba(31,78,104,0.06);
  --chart-ripple: rgba(63,86,57,0.4);
  --chart-dot: var(--moss-dark);

  --click-ripple: rgba(92,122,82,0.3);
  --sync-dot: var(--moss);
  --sync-pulse: rgba(92,122,82,0.3);
  --heatmap-r: 63;
  --heatmap-g: 86;
  --heatmap-b: 57;
  --heatmap-glow-r: 63;
  --heatmap-glow-g: 86;
  --heatmap-glow-b: 57;
  --ambient-r: 92;
  --ambient-g: 122;
  --ambient-b: 82;
```

- [ ] **Step 2: Add matching dark overrides to `.dark` block**

After `--ring: #5C7A52;` add:

```css
  --fg-accent: #8fb87a;

  --bg-nav-blur: rgba(15,26,15,0.85);
  --bg-tooltip: rgba(15,26,15,0.9);
  --border-tooltip: rgba(92,122,82,0.3);

  --chart-grid: rgba(92,122,82,0.08);
  --chart-wave-1: rgba(143,184,122,0.9);
  --chart-fill-1: rgba(92,122,82,0.12);
  --chart-wave-2: rgba(176,95,63,0.7);
  --chart-fill-2: rgba(176,95,63,0.08);
  --chart-wave-3: rgba(31,78,104,0.7);
  --chart-fill-3: rgba(31,78,104,0.08);
  --chart-ripple: rgba(143,184,122,0.4);
  --chart-dot: rgba(143,184,122,0.9);

  --click-ripple: rgba(92,122,82,0.5);
  --sync-dot: #5C7A52;
  --sync-pulse: rgba(92,122,82,0.4);
  --heatmap-r: 92;
  --heatmap-g: 122;
  --heatmap-b: 82;
  --heatmap-glow-r: 143;
  --heatmap-glow-g: 184;
  --heatmap-glow-b: 122;
  --ambient-r: 92;
  --ambient-g: 122;
  --ambient-b: 82;
```

- [ ] **Step 3: Expose new tokens in `@theme inline`**

Add to the `@theme inline` block:

```css
  --color-fg-accent: var(--fg-accent);
```

- [ ] **Step 4: Verify build**

```bash
cd frontend && npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/index.css
git commit -m "feat: add light/dark semantic tokens for charts, nav, tooltips, heatmap"
```

---

### Task 2: Replace hardcoded colors in simple components

**Files:**
- Modify: `frontend/src/components/TimeSavedCard.tsx`
- Modify: `frontend/src/components/MirrorMomentCard.tsx`
- Modify: `frontend/src/components/CollectorCard.tsx`
- Modify: `frontend/src/components/OnboardingChecklist.tsx`
- Modify: `frontend/src/components/SyncIndicator.tsx`

- [ ] **Step 1: TimeSavedCard.tsx**

Replace `style={{ color: '#8fb87a' }}` → `className="... text-fg-accent"`
Replace `style={{ color: '#5C7A52', letterSpacing: '2px' }}` → `className="... text-fg-muted" style={{ letterSpacing: '2px' }}`

- [ ] **Step 2: MirrorMomentCard.tsx**

Replace `style={{ color: '#c8dfc0' }}` → `className="... text-fg"`
(The radial-gradient in the motion.div background uses moss rgba — keep as-is since it's decorative and works on both)

- [ ] **Step 3: CollectorCard.tsx**

Replace `style={{ color: '#c8dfc0' }}` → `className="... text-fg"`
Replace `style={{ color: '#8fb87a' }}` → `className="... text-fg-accent"`
Replace `style={{ color: '#5C7A52' }}` → `className="... text-fg-muted"`
Replace `style={{ color: '#f87171' }}` → `className="... text-fg-error"`

- [ ] **Step 4: OnboardingChecklist.tsx**

Replace `style={{ color: '#c8dfc0' }}` → `className="... text-fg"`
Replace `style={{ color: '#6a9a5a' }}` → `className="... text-fg-secondary"`
Replace `style={{ background: '#5C7A52', color: '#0f1a0f' }}` → `className="... bg-primary text-fg-on-primary"`
Replace `style={{ background: 'rgba(92,122,82,0.15)', color: '#8fb87a' }}` → `className="... bg-accent text-fg-accent"`
Replace `style={{ color: '#8fb87a' }}` → `className="... text-fg-accent"`

- [ ] **Step 5: SyncIndicator.tsx**

Replace inline `style` on outer div with Tailwind classes:
`className="... bg-input border border-default text-fg-secondary"`
Remove the inline style entirely.

- [ ] **Step 6: Update pond.css sync-dot to use tokens**

Replace hardcoded colors in `.sync-dot`:
```css
.sync-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--sync-dot);
  position: relative;
}

.sync-dot::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1px solid var(--sync-pulse);
  animation: syncPulse 2s ease-out infinite;
}
```

- [ ] **Step 7: Verify build**

```bash
cd frontend && npm run build
```

- [ ] **Step 8: Commit**

```bash
git add frontend/src/components/TimeSavedCard.tsx frontend/src/components/MirrorMomentCard.tsx frontend/src/components/CollectorCard.tsx frontend/src/components/OnboardingChecklist.tsx frontend/src/components/SyncIndicator.tsx frontend/src/pond.css
git commit -m "feat: replace hardcoded colors with token classes in simple components"
```

---

### Task 3: Replace hardcoded colors in Layout and pages

**Files:**
- Modify: `frontend/src/components/Layout.tsx`
- Modify: `frontend/src/pages/Login.tsx`
- Modify: `frontend/src/pages/Settings.tsx`
- Modify: `frontend/src/pages/Trends.tsx`
- Modify: `frontend/src/pages/Dashboard.tsx`

- [ ] **Step 1: Layout.tsx**

Replace nav `style={{ background: 'rgba(15,26,15,0.85)', backdropFilter: 'blur(16px)' }}` with `style={{ background: 'var(--bg-nav-blur)', backdropFilter: 'blur(16px)' }}`

Replace logo SVG hardcoded `#8fb87a` fills/strokes with `var(--fg-accent)`. Since SVG inline in JSX can't use Tailwind classes for fill/stroke, use `style={{ fill: 'var(--fg-accent)' }}` and `style={{ stroke: 'var(--fg-accent)' }}`.

Actually simpler: use `currentColor` and set `style={{ color: 'var(--fg-accent)' }}` on the SVG parent, then `fill="currentColor"` / `stroke="currentColor"` on circles.

- [ ] **Step 2: Login.tsx**

Replace all inline `style={{ color: '...' }}` and `style={{ background: '...' }}` with token-based equivalents:
- `style={{ background: 'rgba(248,113,113,0.1)', color: '#f87171' }}` → `className="... bg-error text-fg-error"`
- `style={{ background: 'rgba(92,122,82,0.1)', color: '#8fb87a' }}` → `className="... bg-accent text-fg-accent"`
- Input `style={{ background: 'rgba(92,122,82,0.1)', border: '...' }}` → `className="... bg-input border border-default"`

- [ ] **Step 3: Settings.tsx**

Replace remaining inline styles:
- `style={{ background: 'rgba(92,122,82,0.1)', border: '...' }}` → `className="... bg-input border border-default"`
- `style={{ background: 'rgba(92,122,82,0.06)', border: '...' }}` → `className="... bg-card border border-default"`
- `style={{ background: 'rgba(248,113,113,0.08)', border: '...' }}` → `className="... bg-error border border-default"`
- `style={{ background: '#f87171', color: '#0f1a0f' }}` → `className="... bg-destructive text-fg-on-primary"` (add `--bg-destructive` token if missing)
- `style={{ borderColor: 'rgba(92,122,82,0.2)' }}` → `className="... border-default"`

- [ ] **Step 4: Trends.tsx**

Replace toggle button inline styles with token classes:
- Active: `className="... bg-accent text-fg border border-default"`
- Inactive: `className="... bg-transparent text-fg-secondary border border-subtle"`

- [ ] **Step 5: Dashboard.tsx**

Replace `style={{ letterSpacing: '1.5px' }}` — keep letterSpacing inline (no token needed), use `text-fg-muted` class for color.

- [ ] **Step 6: Verify build and visual test**

```bash
cd frontend && npm run build
```

Toggle light/dark in browser. All text, backgrounds, and borders should adapt.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/Layout.tsx frontend/src/pages/Login.tsx frontend/src/pages/Settings.tsx frontend/src/pages/Trends.tsx frontend/src/pages/Dashboard.tsx
git commit -m "feat: replace hardcoded colors with tokens in Layout and pages"
```

---

### Task 4: Make canvas components read CSS custom properties

**Files:**
- Modify: `frontend/src/components/PondChart.tsx`
- Modify: `frontend/src/components/WaveBarChart.tsx`
- Modify: `frontend/src/components/RippleSpider.tsx`
- Modify: `frontend/src/components/AmbientPond.tsx`
- Modify: `frontend/src/components/ClickRipple.tsx`
- Modify: `frontend/src/components/ActivityPond.tsx`

Canvas and DOM-manipulation components can't use Tailwind classes for drawn elements. Instead, read CSS custom properties at render time via `getComputedStyle(document.documentElement).getPropertyValue('--token-name')`.

- [ ] **Step 1: Create a helper function**

Add at top of each canvas component (or create a shared util `frontend/src/utils/cssVar.ts`):

```typescript
export function cssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}
```

- [ ] **Step 2: PondChart.tsx**

Inside the useEffect, read colors at animation start:

```typescript
const styles = getComputedStyle(document.documentElement)
const gridColor = styles.getPropertyValue('--chart-grid').trim()
const wave1Stroke = styles.getPropertyValue('--chart-wave-1').trim()
const wave1Fill = styles.getPropertyValue('--chart-fill-1').trim()
const wave2Stroke = styles.getPropertyValue('--chart-wave-2').trim()
const wave2Fill = styles.getPropertyValue('--chart-fill-2').trim()
const rippleColor = styles.getPropertyValue('--chart-ripple').trim()
const dotColor = styles.getPropertyValue('--chart-dot').trim()
```

Replace hardcoded `'rgba(92,122,82,0.08)'` with `gridColor`, etc.

Update the tooltip JSX: replace `style={{ color: '#5C7A52' }}` with `className="text-fg-muted"`, `style={{ color: '#8fb87a' }}` with `className="text-fg-accent"`, `style={{ color: '#6a9a5a' }}` with `className="text-fg-secondary"`.

Update the `.pond-tooltip` CSS in pond.css to use tokens:
```css
.pond-tooltip {
  background: var(--bg-tooltip);
  border: 1px solid var(--border-tooltip);
  /* rest stays */
}
```

- [ ] **Step 3: WaveBarChart.tsx**

Read `--fg-secondary`, `--fg-muted` for label colors. Read `--chart-grid` for any grid elements.

```typescript
const fgSecondary = styles.getPropertyValue('--fg-secondary').trim()
const fgMuted = styles.getPropertyValue('--fg-muted').trim()
```

Replace `ctx.fillStyle = '#6a9a5a'` with `ctx.fillStyle = fgSecondary`
Replace `ctx.fillStyle = '#5C7A52'` with `ctx.fillStyle = fgMuted`

- [ ] **Step 4: RippleSpider.tsx**

Read tokens for:
- Grid ring strokes: `--chart-grid`
- Axis lines: `--chart-grid`
- Label colors: `--fg-secondary`, `--fg-accent`
- Data area fill: `--chart-fill-1`
- Data area stroke: `--chart-wave-1`
- Data dots: `--chart-dot`, `--fg-accent`

Replace all hardcoded `'rgba(92,122,82,...)'` and `'#6a9a5a'` / `'#8fb87a'` with the read values.

- [ ] **Step 5: AmbientPond.tsx**

Read `--ambient-r`, `--ambient-g`, `--ambient-b` and construct rgba strings:

```typescript
const r = styles.getPropertyValue('--ambient-r').trim()
const g = styles.getPropertyValue('--ambient-g').trim()
const b = styles.getPropertyValue('--ambient-b').trim()
// Then: `rgba(${r}, ${g}, ${b}, ${alpha})`
```

- [ ] **Step 6: ClickRipple.tsx**

Read `--click-ripple` base opacity from CSS. Or use the `--ambient-r/g/b` tokens:

Replace hardcoded `rgba(92,122,82,...)` with computed values from CSS vars.

- [ ] **Step 7: ActivityPond.tsx**

Read `--heatmap-r/g/b` and `--heatmap-glow-r/g/b` for cell fill and glow colors.

Replace in the cell rendering:
```typescript
backgroundColor: `rgba(${heatR},${heatG},${heatB},${val * 0.8})`
boxShadow: val > 0.7 ? `0 0 ${val * 8}px rgba(${glowR},${glowG},${glowB},${val * 0.4})` : 'none'
```

Replace label inline `style={{ color: '#5C7A52' }}` with `className="text-fg-muted"`.

- [ ] **Step 8: Update pond.css pond-card to use tokens**

```css
.pond-card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  padding: 24px;
  position: relative;
  overflow: hidden;
}
```

- [ ] **Step 9: Verify build and visual test both themes**

```bash
cd frontend && npm run build
```

Toggle light/dark. Canvas charts, ambient background, heatmap, click ripples should all adapt colors.

- [ ] **Step 10: Commit**

```bash
git add frontend/src/components/PondChart.tsx frontend/src/components/WaveBarChart.tsx frontend/src/components/RippleSpider.tsx frontend/src/components/AmbientPond.tsx frontend/src/components/ClickRipple.tsx frontend/src/components/ActivityPond.tsx frontend/src/pond.css
git commit -m "feat: make canvas components read CSS tokens for light/dark support"
```

---

### Task 5: Handle theme changes at runtime

**Files:**
- Modify: canvas components (PondChart, WaveBarChart, RippleSpider, AmbientPond)

- [ ] **Step 1: Re-read CSS vars when theme changes**

Canvas components read CSS vars once at mount. When user toggles theme, the vars change but the canvas keeps using old values. Fix: add the `resolved` theme value from `useTheme()` to the dependency array of the main useEffect, so the canvas re-initializes when theme toggles.

In each canvas component:
```typescript
import { useTheme } from '../context/ThemeContext'
// ...
const { resolved } = useTheme()
// ...
useEffect(() => {
  // ... all the canvas code
}, [data, resolved])  // <-- add resolved
```

This causes the animation to restart with fresh CSS var reads on theme change.

- [ ] **Step 2: Same for ClickRipple and ActivityPond**

ClickRipple creates DOM elements with inline styles — it reads colors at click time, so it automatically picks up the current CSS vars if we change the hardcoded values to `var()` references. No useEffect change needed.

ActivityPond uses inline styles in JSX — re-renders on theme change if parent re-renders. But to be safe, add `resolved` dependency or use CSS var references in the inline styles.

- [ ] **Step 3: Verify theme toggle in browser**

Toggle between light and dark. All canvases should immediately re-render with new colors. No stale colors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/PondChart.tsx frontend/src/components/WaveBarChart.tsx frontend/src/components/RippleSpider.tsx frontend/src/components/AmbientPond.tsx
git commit -m "feat: re-read CSS tokens on theme toggle for canvas components"
```

---

## Visual Test Checklist

After all tasks, verify in browser:

**Light mode:**
- [ ] Page background: warm paper (#EFEAE0)
- [ ] Cards: white with subtle shadow
- [ ] Nav: cream with blur
- [ ] Text: dark ink tones
- [ ] Charts: dark green waves on light background
- [ ] Heatmap: dark green dots
- [ ] Click ripples: subtle green
- [ ] Ambient ripples: barely visible on light background

**Dark mode (pond):**
- [ ] Page background: deep forest (#0f1a0f)
- [ ] Cards: translucent green
- [ ] Nav: dark green with blur
- [ ] Text: light green tones
- [ ] Charts: bright green waves
- [ ] Heatmap: glowing green dots
- [ ] Click ripples: visible green
- [ ] Ambient ripples: subtle green rings

**Theme toggle:**
- [ ] Instant switch, no flash
- [ ] Canvas charts re-render with correct colors
- [ ] No hardcoded colors visible in either mode
