# Pond Design — Frontend Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the rippl dashboard frontend using an organic "pond" visual language where data manifests as ripples on water — dark background, moss-green palette, breathing animations, canvas-based charts.

**Architecture:** Replace the current light-themed Tailwind/Recharts UI with a dark organic theme. New canvas-based `PondChart` replaces Recharts `AreaChart`. New `AmbientPond` background component renders autonomous ripples. All cards get staggered ripple entrance animations via `framer-motion`. Global click ripple effect. The existing Tailwind token system stays but gets a new "pond" dark-only layer. Recharts is kept for secondary charts (bar, pie) but re-themed to match.

**Tech Stack:** React 19, TypeScript, Tailwind CSS v4, framer-motion (new), Canvas 2D API, Vite 8

**Prototype reference:** `docs/rippl-pond-prototype.html` — contains all CSS, animations, and canvas JS to port.

**Design decisions confirmed by user:**
- NO concentric rings behind hero number (rejected as "target-like")
- NO pulsing ripple animation inside stat cards (rejected as "too much")
- Chart hover ripples: exactly ONE ripple per data point (throttled, not spammy)
- Activity heatmap: color density = time_saved_minutes (needs backend query — deferred to later task)
- Click ripple on every page click (moss-green, 3 concentric rings)

---

## File Map

### New files
| File | Purpose |
|------|---------|
| `frontend/src/components/AmbientPond.tsx` | Fixed-position canvas background with autonomous ripple rings |
| `frontend/src/components/PondChart.tsx` | Canvas-based trend chart with breathing waves, hover ripples, tooltip |
| `frontend/src/components/ActivityPond.tsx` | Heatmap grid (7 days x 24 hours) with circular dots and hover ripple |
| `frontend/src/components/ClickRipple.tsx` | Global click handler that spawns concentric ring animations at cursor |
| `frontend/src/components/SyncIndicator.tsx` | Nav pill with pulsing dot showing last sync time |
| `frontend/src/pond.css` | Pond-specific CSS keyframes and classes (ripple cards, sync pulse, heatmap) |

### Modified files
| File | What changes |
|------|-------------|
| `frontend/package.json` | Add `framer-motion` dependency |
| `frontend/src/index.css` | Replace light/dark semantic tokens with pond dark theme |
| `frontend/src/components/Layout.tsx` | Dark nav with backdrop blur, rippl icon in green, sync indicator, add AmbientPond + ClickRipple |
| `frontend/src/components/TimeSavedCard.tsx` | Restyle as clean hero stat (big number, label, subtitle — no rings) |
| `frontend/src/components/MirrorMomentCard.tsx` | Ripple card entrance via framer-motion, radial background expansion |
| `frontend/src/components/TrendChart.tsx` | Replace with PondChart wrapper (keep as adapter for data transformation) |
| `frontend/src/components/CollectorCard.tsx` | Restyle with pond card appearance |
| `frontend/src/components/OnboardingChecklist.tsx` | Restyle for dark pond theme |
| `frontend/src/pages/Dashboard.tsx` | Use new stat row layout, PondChart, framer-motion stagger on mirror cards |
| `frontend/src/pages/Trends.tsx` | PondChart, restyle bar/pie with dark theme, add ActivityPond |
| `frontend/src/pages/Mirror.tsx` | Staggered ripple card entrance |
| `frontend/src/pages/Settings.tsx` | Restyle cards and buttons for pond theme |
| `frontend/src/pages/Login.tsx` | Dark background, pond-styled card |
| `frontend/src/context/ThemeContext.tsx` | Remove theme toggle (pond is always dark) or keep as pond-only |
| `frontend/index.html` | Update background color to `#0f1a0f` for flash prevention |

---

## Color Reference

All pond colors (extracted from prototype):

| Token | Value | Usage |
|-------|-------|-------|
| `--pond-bg` | `#0f1a0f` | Page background |
| `--pond-card` | `rgba(92,122,82,0.06)` | Card background |
| `--pond-card-border` | `rgba(92,122,82,0.15)` | Card border |
| `--pond-nav` | `rgba(15,26,15,0.85)` | Nav background (with backdrop-filter: blur(16px)) |
| `--pond-nav-border` | `rgba(92,122,82,0.15)` | Nav bottom border |
| `--pond-fg` | `#c8dfc0` | Primary text |
| `--pond-fg-secondary` | `#6a9a5a` | Secondary text, nav links |
| `--pond-fg-muted` | `#5C7A52` | Labels, uppercase text |
| `--pond-fg-accent` | `#8fb87a` | Stat numbers, strong text, data highlights |
| `--pond-terra` | `#B05F3F` | Secondary accent (ChatGPT wave, contrast highlights) |
| `--pond-input-bg` | `rgba(92,122,82,0.1)` | Input/button backgrounds |
| `--pond-input-border` | `rgba(92,122,82,0.2)` | Input/button borders |
| `--pond-tooltip-bg` | `rgba(15,26,15,0.9)` | Tooltip background (with backdrop-filter: blur(12px)) |
| `--pond-tooltip-border` | `rgba(92,122,82,0.3)` | Tooltip border |

---

### Task 1: Install framer-motion and create pond.css

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/pond.css`
- Modify: `frontend/src/main.tsx` (import pond.css)

- [ ] **Step 1: Install framer-motion**

```bash
cd frontend && npm install framer-motion
```

- [ ] **Step 2: Create pond.css with keyframes and utility classes**

Create `frontend/src/pond.css`:

```css
/* ===== Pond Keyframes ===== */

@keyframes clickRippleAnim {
  0% { width: 0; height: 0; top: 0; left: 0; opacity: 1; }
  100% { width: 80px; height: 80px; top: -40px; left: -40px; opacity: 0; }
}

@keyframes syncPulse {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(2.5); opacity: 0; }
}

@keyframes heatRipple {
  0% { transform: scale(0.5); opacity: 1; }
  100% { transform: scale(2); opacity: 0; }
}

/* ===== Pond Card ===== */
.pond-card {
  background: rgba(92,122,82,0.06);
  border: 1px solid rgba(92,122,82,0.15);
  border-radius: 16px;
  padding: 24px;
  position: relative;
  overflow: hidden;
}

/* ===== Sync Indicator ===== */
.sync-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #5C7A52;
  position: relative;
}

.sync-dot::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1px solid rgba(92,122,82,0.4);
  animation: syncPulse 2s ease-out infinite;
}

/* ===== Heatmap Cell ===== */
.heatmap-cell {
  aspect-ratio: 1;
  border-radius: 50%;
  transition: all 0.3s;
  cursor: pointer;
  position: relative;
}

.heatmap-cell:hover {
  transform: scale(1.8);
  z-index: 2;
}

.heatmap-cell:hover::after {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 1px solid rgba(143,184,122,0.3);
  animation: heatRipple 0.6s ease-out;
}

/* ===== Pond Surface ===== */
.pond-surface {
  position: relative;
  height: 280px;
  cursor: crosshair;
}

.pond-surface canvas {
  width: 100%;
  height: 100%;
  border-radius: 12px;
}

/* ===== Pond Tooltip ===== */
.pond-tooltip {
  position: absolute;
  background: rgba(15,26,15,0.9);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(92,122,82,0.3);
  border-radius: 12px;
  padding: 12px 16px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 10;
  min-width: 140px;
}

.pond-tooltip.visible {
  opacity: 1;
}
```

- [ ] **Step 3: Import pond.css in main.tsx**

Add to `frontend/src/main.tsx` after the `index.css` import:

```typescript
import './pond.css'
```

- [ ] **Step 4: Verify build succeeds**

```bash
cd frontend && npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/src/pond.css frontend/src/main.tsx
git commit -m "chore: add framer-motion and pond CSS foundation"
```

---

### Task 2: Replace CSS tokens with pond dark theme

**Files:**
- Modify: `frontend/src/index.css`
- Modify: `frontend/index.html` (flash prevention)

- [ ] **Step 1: Replace the entire :root and .dark token blocks in index.css**

Replace the two `:root` blocks and the `.dark` block with a single `:root` block. Keep the `@import "tailwindcss"` and the `@theme inline` block (updated for new tokens).

New `:root`:

```css
:root {
  --moss: #5C7A52;
  --moss-dark: #3F5639;
  --moss-tint: #8fb87a;
  --terra: #B05F3F;
  --terra-dark: #8B4830;
  --cream: #c8dfc0;
  --ink: #0f1a0f;
  --ink-2: #6a9a5a;
  --ink-3: #5C7A52;
  --line: rgba(92,122,82,0.15);
  --line-2: rgba(92,122,82,0.08);
  --destructive: #f87171;

  --bg-page: #0f1a0f;
  --bg-card: rgba(92,122,82,0.06);
  --bg-nav: rgba(15,26,15,0.85);
  --bg-input: rgba(92,122,82,0.1);
  --bg-muted: rgba(92,122,82,0.08);
  --bg-accent: rgba(92,122,82,0.15);
  --bg-error: rgba(180,50,50,0.15);
  --bg-success: rgba(92,122,82,0.15);

  --fg: #c8dfc0;
  --fg-secondary: #6a9a5a;
  --fg-muted: #5C7A52;
  --fg-on-primary: #0f1a0f;
  --fg-error: #f87171;
  --fg-success: #8fb87a;
  --fg-active: #8fb87a;

  --border-default: rgba(92,122,82,0.15);
  --border-subtle: rgba(92,122,82,0.08);

  --primary: #5C7A52;
  --primary-hover: #8fb87a;
  --secondary: #B05F3F;
  --ring: #5C7A52;

  --font-sans: 'Inter', system-ui, sans-serif;
  --font-serif: 'Source Serif 4', Georgia, serif;

  --radius-card: 16px;
  --radius-input: 12px;
  --radius-pill: 999px;

  --shadow-card: none;
}
```

Remove the `.dark { ... }` block entirely (pond is always dark).

- [ ] **Step 2: Update @theme inline block**

Keep the existing `@theme inline` block as-is — it references the CSS variables, so it automatically picks up the new values.

- [ ] **Step 3: Update index.html flash prevention**

Replace the `<script>` block in `frontend/index.html`:

```html
<script>
  document.documentElement.classList.add('dark');
</script>
```

Change the `<body>` background to match:

```html
<body style="background:#0f1a0f">
```

- [ ] **Step 4: Verify build succeeds**

```bash
cd frontend && npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/index.css frontend/index.html
git commit -m "feat: replace light/dark tokens with pond dark theme"
```

---

### Task 3: Create AmbientPond background component

**Files:**
- Create: `frontend/src/components/AmbientPond.tsx`

- [ ] **Step 1: Create AmbientPond.tsx**

```tsx
import { useEffect, useRef } from 'react'

interface Ripple {
  x: number
  y: number
  r: number
  maxR: number
  speed: number
  opacity: number
}

export function AmbientPond() {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    let w = 0
    let h = 0
    const ripples: Ripple[] = []
    let animId = 0

    function resize() {
      w = canvas!.width = window.innerWidth
      h = canvas!.height = window.innerHeight
    }
    resize()
    window.addEventListener('resize', resize)

    function addRipple() {
      ripples.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r: 0,
        maxR: 80 + Math.random() * 120,
        speed: 0.3 + Math.random() * 0.4,
        opacity: 0.06 + Math.random() * 0.04,
      })
    }

    const interval = setInterval(addRipple, 3000)
    for (let i = 0; i < 4; i++) addRipple()

    function draw() {
      ctx!.clearRect(0, 0, w, h)
      for (let i = ripples.length - 1; i >= 0; i--) {
        const rip = ripples[i]
        rip.r += rip.speed
        const progress = rip.r / rip.maxR
        if (progress > 1) { ripples.splice(i, 1); continue }
        const alpha = rip.opacity * (1 - progress)
        ctx!.beginPath()
        ctx!.arc(rip.x, rip.y, rip.r, 0, Math.PI * 2)
        ctx!.strokeStyle = `rgba(92, 122, 82, ${alpha})`
        ctx!.lineWidth = 1
        ctx!.stroke()
        if (rip.r > 20) {
          ctx!.beginPath()
          ctx!.arc(rip.x, rip.y, rip.r * 0.6, 0, Math.PI * 2)
          ctx!.strokeStyle = `rgba(92, 122, 82, ${alpha * 0.6})`
          ctx!.stroke()
        }
      }
      animId = requestAnimationFrame(draw)
    }
    draw()

    return () => {
      window.removeEventListener('resize', resize)
      clearInterval(interval)
      cancelAnimationFrame(animId)
    }
  }, [])

  return (
    <canvas
      ref={canvasRef}
      className="fixed inset-0 z-0 pointer-events-none"
      style={{ width: '100%', height: '100%' }}
    />
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/AmbientPond.tsx
git commit -m "feat: add AmbientPond canvas background component"
```

---

### Task 4: Create ClickRipple global effect component

**Files:**
- Create: `frontend/src/components/ClickRipple.tsx`

- [ ] **Step 1: Create ClickRipple.tsx**

```tsx
import { useEffect } from 'react'

export function ClickRipple() {
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      const container = document.createElement('div')
      container.style.cssText = `position:fixed;left:${e.clientX}px;top:${e.clientY}px;pointer-events:none;z-index:9999;`
      for (let i = 0; i < 3; i++) {
        const ring = document.createElement('div')
        ring.style.cssText = `
          position:absolute;border-radius:50%;
          border:${1.5 - i * 0.4}px solid rgba(92,122,82,${0.5 - i * 0.15});
          animation:clickRippleAnim ${0.8 + i * 0.3}s ease-out forwards;
        `
        container.appendChild(ring)
      }
      document.body.appendChild(container)
      setTimeout(() => container.remove(), 1500)
    }

    document.addEventListener('click', handleClick)
    return () => document.removeEventListener('click', handleClick)
  }, [])

  return null
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/ClickRipple.tsx
git commit -m "feat: add ClickRipple global micro-interaction"
```

---

### Task 5: Create SyncIndicator component

**Files:**
- Create: `frontend/src/components/SyncIndicator.tsx`

- [ ] **Step 1: Create SyncIndicator.tsx**

This component fetches the last sync time from the collectors API and displays a pulsing dot.

```tsx
import { useEffect, useState } from 'react'
import { getCollectors } from '../api/collectors'

export function SyncIndicator() {
  const [lastSync, setLastSync] = useState<string | null>(null)

  useEffect(() => {
    getCollectors().then(collectors => {
      const synced = collectors.find(c => c.lastSyncAt)
      if (synced?.lastSyncAt) setLastSync(synced.lastSyncAt)
    }).catch(() => {})
  }, [])

  if (!lastSync) return null

  const time = new Date(lastSync)
  const diff = Math.round((Date.now() - time.getTime()) / 60000)
  const label = diff < 1 ? 'just now' : diff < 60 ? `${diff} min ago` : `${Math.round(diff / 60)}h ago`

  return (
    <div className="flex items-center gap-2 px-3 py-1.5 rounded-full"
      style={{ background: 'rgba(92,122,82,0.1)', border: '1px solid rgba(92,122,82,0.2)', fontSize: '12px', color: '#6a9a5a' }}>
      <div className="sync-dot" />
      <span>synced {label}</span>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/SyncIndicator.tsx
git commit -m "feat: add SyncIndicator with pulsing dot"
```

---

### Task 6: Restyle Layout (nav, shell, ambient background)

**Files:**
- Modify: `frontend/src/components/Layout.tsx`
- Modify: `frontend/src/context/ThemeContext.tsx`

- [ ] **Step 1: Update Layout.tsx**

Replace the entire Layout component. Key changes:
- Dark nav with `backdrop-filter: blur(16px)`
- Rippl logo icon uses `#8fb87a` instead of `currentColor`
- Add SyncIndicator to nav
- Add AmbientPond as fixed background
- Add ClickRipple for global click effect
- Remove theme toggle button (pond is always dark)

```tsx
import { NavLink, Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { AmbientPond } from './AmbientPond'
import { ClickRipple } from './ClickRipple'
import { SyncIndicator } from './SyncIndicator'

export function Layout() {
  const { user, loading, logout } = useAuth()

  if (loading) return <div className="min-h-screen flex items-center justify-center" style={{ background: '#0f1a0f', color: '#5C7A52' }}>Loading...</div>
  if (!user) return <Navigate to="/login" replace />

  return (
    <div className="min-h-screen" style={{ background: '#0f1a0f' }}>
      <AmbientPond />
      <ClickRipple />
      <nav className="fixed top-0 left-0 right-0 z-50 border-b"
        style={{ background: 'rgba(15,26,15,0.85)', backdropFilter: 'blur(16px)', borderColor: 'rgba(92,122,82,0.15)' }}>
        <div className="max-w-5xl mx-auto px-4 py-3 flex justify-between items-center">
          <NavLink to="/" className="flex items-center gap-2 text-xl font-bold font-serif" style={{ color: '#c8dfc0' }}>
            <svg width="22" height="22" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="16" cy="16" r="3.2" fill="#8fb87a"/>
              <circle cx="16" cy="16" r="8.7" stroke="#8fb87a" strokeWidth="1.8" opacity=".55"/>
              <circle cx="16" cy="16" r="14" stroke="#8fb87a" strokeWidth="1.5" opacity=".25"/>
            </svg>
            rippl
          </NavLink>
          <div className="flex gap-6 items-center">
            <SyncIndicator />
            <NavLink to="/trends" className={({ isActive }) =>
              isActive ? 'font-medium' : 'hover:text-fg'
            } style={({ isActive }) => ({ color: isActive ? '#c8dfc0' : '#6a9a5a' })}>Trends</NavLink>
            <NavLink to="/mirror" className={({ isActive }) =>
              isActive ? 'font-medium' : 'hover:text-fg'
            } style={({ isActive }) => ({ color: isActive ? '#c8dfc0' : '#6a9a5a' })}>Mirror</NavLink>
            <NavLink to="/settings" className={({ isActive }) =>
              isActive ? 'font-medium' : 'hover:text-fg'
            } style={({ isActive }) => ({ color: isActive ? '#c8dfc0' : '#6a9a5a' })}>Settings</NavLink>
            <button onClick={logout} className="text-sm" style={{ color: '#5C7A52' }}>
              Sign out
            </button>
          </div>
        </div>
      </nav>
      <main className="relative z-10 max-w-5xl mx-auto px-4 pt-20 pb-8">
        <Outlet />
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Simplify ThemeContext.tsx**

Since pond is always dark, simplify to just set dark mode. Keep the provider wrapper for compatibility but remove system/light logic:

```tsx
import { createContext, useContext, type ReactNode } from 'react'

interface ThemeState {
  theme: 'dark'
  resolved: 'dark'
  setTheme: (t: string) => void
}

const ThemeContext = createContext<ThemeState>({
  theme: 'dark',
  resolved: 'dark',
  setTheme: () => {},
})

export function useTheme() {
  return useContext(ThemeContext)
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  return (
    <ThemeContext.Provider value={{ theme: 'dark', resolved: 'dark', setTheme: () => {} }}>
      {children}
    </ThemeContext.Provider>
  )
}
```

- [ ] **Step 3: Start dev server and verify nav renders correctly**

```bash
cd frontend && npm run dev
```

Open browser, check: dark background, blurred nav, green rippl icon, sync indicator, ambient ripples in background, click ripple on any click.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/Layout.tsx frontend/src/context/ThemeContext.tsx
git commit -m "feat: restyle Layout with pond nav, ambient background, click ripple"
```

---

### Task 7: Create PondChart canvas component

**Files:**
- Create: `frontend/src/components/PondChart.tsx`

- [ ] **Step 1: Create PondChart.tsx**

This is the core canvas chart. Port the `initPondChart` logic from the prototype into a React component with refs and useEffect.

```tsx
import { useEffect, useRef, useState } from 'react'
import type { WeeklyTrend } from '../types'
import { getDomain } from '../data/domains'

interface Ripple {
  x: number; y: number; r: number; opacity: number
}

interface TooltipState {
  visible: boolean
  x: number; y: number
  week: string; total: number
  breakdown: { domain: string; minutes: number }[]
}

export function PondChart({ data }: { data: WeeklyTrend[] }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const [tooltip, setTooltip] = useState<TooltipState>({ visible: false, x: 0, y: 0, week: '', total: 0, breakdown: [] })

  const domains = [...new Set(data.map(d => d.domain))]
  const byWeek: Record<string, Record<string, number>> = {}
  for (const d of data) {
    if (!byWeek[d.week]) byWeek[d.week] = {}
    byWeek[d.week][d.domain] = Math.round(d.totalSeconds / 60)
  }
  const weeks = Object.keys(byWeek).sort()
  const maxVal = Math.max(20, ...weeks.flatMap(w => domains.map(d => byWeek[w][d] ?? 0))) * 1.2

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || weeks.length === 0) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const W = canvas.width
    const H = canvas.height
    let time = 0
    let animId = 0
    let lastHoveredWeek = -1
    const activeRipples: Ripple[] = []

    function getY(val: number) { return H - (val / maxVal) * (H - 40) - 20 }
    function getX(i: number) { return 40 + (i / Math.max(weeks.length - 1, 1)) * (W - 80) }

    function drawWave(values: number[], color: string, fillColor: string, offset: number) {
      const points = values.map((v, i) => ({
        x: getX(i),
        y: getY(v) + Math.sin(time * 0.02 + i * 0.8 + offset) * 3,
      }))

      // Fill
      ctx!.beginPath()
      ctx!.moveTo(points[0].x, points[0].y)
      for (let i = 1; i < points.length; i++) {
        const cpx = (points[i-1].x + points[i].x) / 2
        ctx!.bezierCurveTo(cpx, points[i-1].y, cpx, points[i].y, points[i].x, points[i].y)
      }
      ctx!.lineTo(points[points.length-1].x, H)
      ctx!.lineTo(points[0].x, H)
      ctx!.closePath()
      ctx!.fillStyle = fillColor
      ctx!.fill()

      // Stroke
      ctx!.beginPath()
      ctx!.moveTo(points[0].x, points[0].y)
      for (let i = 1; i < points.length; i++) {
        const cpx = (points[i-1].x + points[i].x) / 2
        ctx!.bezierCurveTo(cpx, points[i-1].y, cpx, points[i].y, points[i].x, points[i].y)
      }
      ctx!.strokeStyle = color
      ctx!.lineWidth = 2
      ctx!.stroke()

      // Dots
      for (const p of points) {
        ctx!.beginPath()
        ctx!.arc(p.x, p.y, 3, 0, Math.PI * 2)
        ctx!.fillStyle = color
        ctx!.fill()
      }

      return points
    }

    // Domain colors for pond theme
    const domainColors: Record<string, { stroke: string; fill: string }> = {}
    const colorPairs = [
      { stroke: 'rgba(143,184,122,0.9)', fill: 'rgba(92,122,82,0.12)' },
      { stroke: 'rgba(176,95,63,0.7)', fill: 'rgba(176,95,63,0.08)' },
      { stroke: 'rgba(31,78,104,0.7)', fill: 'rgba(31,78,104,0.08)' },
      { stroke: 'rgba(139,72,48,0.7)', fill: 'rgba(139,72,48,0.08)' },
    ]
    domains.forEach((d, i) => { domainColors[d] = colorPairs[i % colorPairs.length] })

    function animate() {
      time++
      ctx!.clearRect(0, 0, W, H)

      // Grid lines
      ctx!.strokeStyle = 'rgba(92,122,82,0.08)'
      ctx!.lineWidth = 0.5
      for (let i = 0; i <= 4; i++) {
        const y = 20 + i * (H - 40) / 4
        ctx!.beginPath(); ctx!.moveTo(40, y); ctx!.lineTo(W - 40, y); ctx!.stroke()
      }

      // Draw waves (reverse order so first domain is on top)
      for (let di = domains.length - 1; di >= 0; di--) {
        const domain = domains[di]
        const values = weeks.map(w => byWeek[w][domain] ?? 0)
        const colors = domainColors[domain]
        drawWave(values, colors.stroke, colors.fill, di * 2)
      }

      // Ripples
      for (let i = activeRipples.length - 1; i >= 0; i--) {
        const rip = activeRipples[i]
        rip.r += 0.8
        rip.opacity -= 0.008
        if (rip.opacity <= 0) { activeRipples.splice(i, 1); continue }
        ctx!.beginPath()
        ctx!.arc(rip.x, rip.y, rip.r, 0, Math.PI * 2)
        ctx!.strokeStyle = `rgba(143, 184, 122, ${rip.opacity})`
        ctx!.lineWidth = 1
        ctx!.stroke()
      }

      animId = requestAnimationFrame(animate)
    }

    function handleMouseMove(e: MouseEvent) {
      const rect = canvas!.getBoundingClientRect()
      const mx = (e.clientX - rect.left) * (W / rect.width)

      let closest = 0
      let closestDist = Infinity
      for (let i = 0; i < weeks.length; i++) {
        const d = Math.abs(getX(i) - mx)
        if (d < closestDist) { closestDist = d; closest = i }
      }

      if (closestDist < 40) {
        const week = weeks[closest]
        const breakdown = domains.map(d => ({ domain: getDomain(d).name, minutes: byWeek[week][d] ?? 0 })).filter(b => b.minutes > 0)
        const total = breakdown.reduce((s, b) => s + b.minutes, 0)
        setTooltip({
          visible: true,
          x: e.clientX - rect.left + 16,
          y: e.clientY - rect.top - 60,
          week: new Date(week).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
          total,
          breakdown,
        })

        if (closest !== lastHoveredWeek) {
          lastHoveredWeek = closest
          const primaryValues = weeks.map(w => byWeek[w][domains[0]] ?? 0)
          activeRipples.push({ x: getX(closest), y: getY(primaryValues[closest]), r: 3, opacity: 0.35 })
        }
      } else {
        setTooltip(prev => ({ ...prev, visible: false }))
      }
    }

    function handleMouseLeave() {
      setTooltip(prev => ({ ...prev, visible: false }))
      lastHoveredWeek = -1
    }

    function handleClick(e: MouseEvent) {
      const rect = canvas!.getBoundingClientRect()
      const cx = (e.clientX - rect.left) * (W / rect.width)
      const cy = (e.clientY - rect.top) * (H / rect.height)
      for (let i = 0; i < 3; i++) {
        activeRipples.push({ x: cx, y: cy, r: i * 8, opacity: 0.5 - i * 0.1 })
      }
    }

    canvas.addEventListener('mousemove', handleMouseMove)
    canvas.addEventListener('mouseleave', handleMouseLeave)
    canvas.addEventListener('click', handleClick)
    animate()

    return () => {
      cancelAnimationFrame(animId)
      canvas.removeEventListener('mousemove', handleMouseMove)
      canvas.removeEventListener('mouseleave', handleMouseLeave)
      canvas.removeEventListener('click', handleClick)
    }
  }, [data])

  if (weeks.length === 0) return null

  const weekLabels = weeks.filter((_, i) => i % Math.max(1, Math.floor(weeks.length / 5)) === 0)

  return (
    <div className="pond-card" ref={containerRef}>
      <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1px' }}>
        AI Usage (minutes/week)
      </p>
      <div className="pond-surface">
        <canvas ref={canvasRef} width={936} height={280} />
        <div className={`pond-tooltip ${tooltip.visible ? 'visible' : ''}`}
          style={{ left: tooltip.x, top: tooltip.y }}>
          <div className="text-xs mb-1" style={{ color: '#5C7A52' }}>Week of {tooltip.week}</div>
          <div className="text-lg font-semibold" style={{ color: '#8fb87a' }}>{tooltip.total} min</div>
          {tooltip.breakdown.map(b => (
            <div key={b.domain} className="text-xs mt-0.5" style={{ color: '#6a9a5a' }}>
              {b.domain}: {b.minutes}
            </div>
          ))}
        </div>
      </div>
      <div className="flex justify-between pt-2">
        {weekLabels.map(w => (
          <span key={w} className="text-xs" style={{ color: '#5C7A52' }}>
            {new Date(w).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
          </span>
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Verify in browser with dev server**

Navigate to Dashboard or Trends page. Chart should show breathing waves, hover to see tooltip + ripple at data point, click to create ripple.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/PondChart.tsx
git commit -m "feat: add PondChart canvas component with breathing waves and hover ripples"
```

---

### Task 8: Create ActivityPond heatmap component

**Files:**
- Create: `frontend/src/components/ActivityPond.tsx`

- [ ] **Step 1: Create ActivityPond.tsx**

For now this uses mock data since the backend doesn't yet have an hourly breakdown endpoint. The component is built to accept real data via props when available.

```tsx
interface ActivityPondProps {
  data?: number[][]  // 7 days x 24 hours, values 0-1
}

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

const MOCK_DATA = [
  [0,0,0,0,0,0,0,0.2,0.5,0.8,0.9,0.7,0.4,0.6,0.8,0.9,0.7,0.5,0.3,0.1,0,0,0,0],
  [0,0,0,0,0,0,0,0.3,0.6,0.7,0.8,0.9,0.5,0.7,0.9,1.0,0.8,0.6,0.4,0.2,0,0,0,0],
  [0,0,0,0,0,0,0.1,0.4,0.7,0.9,1.0,1.0,0.6,0.8,1.0,1.0,0.9,0.7,0.5,0.3,0.1,0,0,0],
  [0,0,0,0,0,0,0,0.3,0.5,0.7,0.8,0.6,0.3,0.5,0.7,0.8,0.6,0.4,0.2,0.1,0,0,0,0],
  [0,0,0,0,0,0,0,0.2,0.4,0.6,0.7,0.5,0.3,0.4,0.6,0.5,0.3,0.2,0.1,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0.1,0.2,0.3,0.2,0.1,0.2,0.1,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0.1,0.1,0,0,0.1,0,0,0,0,0,0,0,0,0],
]

export function ActivityPond({ data }: ActivityPondProps) {
  const grid = data ?? MOCK_DATA

  return (
    <div className="pond-card">
      <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1px' }}>
        Weekly Activity Pattern
      </p>
      <div className="flex">
        <div className="flex flex-col justify-between mr-2" style={{ gap: '3px' }}>
          {DAYS.map(d => (
            <div key={d} className="text-[10px] flex items-center" style={{ color: '#5C7A52', height: '100%' }}>{d}</div>
          ))}
        </div>
        <div className="flex-1">
          <div className="grid gap-[3px]" style={{ gridTemplateColumns: 'repeat(24, 1fr)' }}>
            {grid.flatMap((dayRow, day) =>
              dayRow.map((val, hour) => (
                <div
                  key={`${day}-${hour}`}
                  className="heatmap-cell"
                  title={`${DAYS[day]} ${hour}:00 — ${Math.round(val * 60)} min`}
                  style={{
                    backgroundColor: `rgba(92,122,82,${val * 0.8})`,
                    boxShadow: val > 0.7 ? `0 0 ${val * 8}px rgba(143,184,122,${val * 0.4})` : 'none',
                  }}
                />
              ))
            )}
          </div>
          <div className="flex justify-between mt-2">
            {['0:00', '6:00', '12:00', '18:00', '23:00'].map(t => (
              <span key={t} className="text-[10px]" style={{ color: '#5C7A52' }}>{t}</span>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/ActivityPond.tsx
git commit -m "feat: add ActivityPond heatmap component with ripple hover"
```

---

### Task 9: Restyle TimeSavedCard as hero stat

**Files:**
- Modify: `frontend/src/components/TimeSavedCard.tsx`

- [ ] **Step 1: Replace TimeSavedCard with clean hero stat**

```tsx
import type { TimeSaved } from '../types'

export function TimeSavedCard({ data }: { data: TimeSaved }) {
  const hours = Math.floor(data.total / 60)
  const minutes = data.total % 60
  const display = hours > 0 ? `${hours},${Math.round((minutes / 60) * 10)}h` : `${minutes}m`

  return (
    <div className="flex flex-col items-center py-8">
      <p className="text-6xl font-bold font-serif" style={{ color: '#8fb87a' }}>{display}</p>
      <p className="text-sm uppercase tracking-widest mt-1" style={{ color: '#5C7A52', letterSpacing: '2px' }}>
        Time Saved
      </p>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/TimeSavedCard.tsx
git commit -m "feat: restyle TimeSavedCard as clean pond hero stat"
```

---

### Task 10: Restyle MirrorMomentCard with framer-motion ripple entrance

**Files:**
- Modify: `frontend/src/components/MirrorMomentCard.tsx`

- [ ] **Step 1: Replace MirrorMomentCard**

```tsx
import { motion } from 'framer-motion'
import type { MirrorMoment } from '../types'

const TYPE_ICONS: Record<string, string> = {
  weekly_usage: '📈',
  top_tool: '🎯',
  time_saving_activity: '⏱️',
  busiest_day: '📅',
}

export function MirrorMomentCard({ moment, index = 0 }: { moment: MirrorMoment; index?: number }) {
  return (
    <motion.div
      className="pond-card overflow-hidden"
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.6, delay: index * 0.15, ease: [0.4, 0, 0.2, 1] }}
    >
      <div className="absolute inset-0 pointer-events-none">
        <motion.div
          className="absolute rounded-full"
          style={{
            top: '50%', left: '50%',
            background: 'radial-gradient(circle, rgba(92,122,82,0.15), transparent 70%)',
          }}
          initial={{ width: 0, height: 0, x: 0, y: 0 }}
          animate={{ width: 400, height: 400, x: -200, y: -200 }}
          transition={{ duration: 1.5, delay: index * 0.15, ease: 'easeOut' }}
        />
      </div>
      <span className="text-xl relative z-10">{TYPE_ICONS[moment.type] ?? '💡'}</span>
      <p className="text-sm mt-2 leading-relaxed relative z-10" style={{ color: '#c8dfc0' }}>
        {moment.message}
      </p>
    </motion.div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/MirrorMomentCard.tsx
git commit -m "feat: restyle MirrorMomentCard with framer-motion ripple entrance"
```

---

### Task 11: Restyle CollectorCard for pond theme

**Files:**
- Modify: `frontend/src/components/CollectorCard.tsx`

- [ ] **Step 1: Replace CollectorCard**

```tsx
import type { CollectorInfo } from '../types'

interface Props {
  collector: CollectorInfo
  onRemove: (id: string) => void
}

const TYPE_LABELS: Record<string, string> = {
  chrome_extension: 'Chrome Extension',
}

export function CollectorCard({ collector, onRemove }: Props) {
  return (
    <div className="pond-card flex items-center justify-between">
      <div>
        <p className="font-medium" style={{ color: '#c8dfc0' }}>{TYPE_LABELS[collector.type] ?? collector.type}</p>
        {collector.lastSyncAt ? (
          <p className="text-sm" style={{ color: '#8fb87a' }}>
            Connected — last sync at {new Date(collector.lastSyncAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </p>
        ) : (
          <p className="text-sm" style={{ color: '#5C7A52' }}>
            Pending — waiting for first sync
          </p>
        )}
      </div>
      <button
        onClick={() => onRemove(collector.id)}
        className="text-sm"
        style={{ color: '#f87171' }}
      >
        Remove
      </button>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/CollectorCard.tsx
git commit -m "feat: restyle CollectorCard for pond theme"
```

---

### Task 12: Restyle OnboardingChecklist for pond theme

**Files:**
- Modify: `frontend/src/components/OnboardingChecklist.tsx`

- [ ] **Step 1: Replace OnboardingChecklist**

```tsx
export function OnboardingChecklist() {
  return (
    <div className="max-w-md mx-auto mt-16 text-center">
      <h2 className="text-2xl font-bold font-serif" style={{ color: '#c8dfc0' }}>Welcome to rippl</h2>
      <p className="mt-2" style={{ color: '#6a9a5a' }}>Let's get you set up</p>
      <div className="mt-8 space-y-4 text-left">
        <Step number={1} title="Connect a data source" description="Install the Chrome extension to start tracking your AI usage." />
        <Step number={2} title="Use AI for a day" description="Browse your favorite AI tools. We'll capture your sessions automatically." />
        <Step number={3} title="See your insights" description="Come back here to explore trends, time saved, and mirror moments." />
      </div>
      <a
        href="/settings"
        className="inline-block mt-8 px-6 py-2 rounded-xl text-sm"
        style={{ background: '#5C7A52', color: '#0f1a0f' }}
      >
        Go to Settings to connect
      </a>
    </div>
  )
}

function Step({ number, title, description }: { number: number; title: string; description: string }) {
  return (
    <div className="flex gap-4 items-start">
      <div className="w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm flex-shrink-0"
        style={{ background: 'rgba(92,122,82,0.15)', color: '#8fb87a' }}>
        {number}
      </div>
      <div>
        <p className="font-medium" style={{ color: '#c8dfc0' }}>{title}</p>
        <p className="text-sm" style={{ color: '#6a9a5a' }}>{description}</p>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/OnboardingChecklist.tsx
git commit -m "feat: restyle OnboardingChecklist for pond theme"
```

---

### Task 13: Update Dashboard page with stat row and PondChart

**Files:**
- Modify: `frontend/src/pages/Dashboard.tsx`

- [ ] **Step 1: Replace Dashboard.tsx**

```tsx
import { useEffect, useState } from 'react'
import { getWeeklyTrends, getTimeSaved } from '../api/trends'
import { getMirrorMoments } from '../api/insights'
import type { WeeklyTrend, TimeSaved, MirrorMoment } from '../types'
import { TimeSavedCard } from '../components/TimeSavedCard'
import { PondChart } from '../components/PondChart'
import { MirrorMomentCard } from '../components/MirrorMomentCard'
import { OnboardingChecklist } from '../components/OnboardingChecklist'

export function Dashboard() {
  const [trends, setTrends] = useState<WeeklyTrend[]>([])
  const [timeSaved, setTimeSaved] = useState<TimeSaved | null>(null)
  const [insights, setInsights] = useState<MirrorMoment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      getWeeklyTrends(),
      getTimeSaved(),
      getMirrorMoments(),
    ]).then(([t, ts, i]) => {
      setTrends(t)
      setTimeSaved(ts)
      setInsights(i)
    }).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16" style={{ color: '#5C7A52' }}>Loading dashboard...</div>
  if (!timeSaved || timeSaved.total === 0) return <OnboardingChecklist />

  return (
    <div className="space-y-8">
      <TimeSavedCard data={timeSaved} />
      <PondChart data={trends} />
      {insights.length > 0 && (
        <div>
          <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1.5px' }}>
            Mirror Moments
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {insights.map((m, i) => <MirrorMomentCard key={i} moment={m} index={i} />)}
          </div>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Verify in browser**

Open Dashboard. Should show: hero stat centered, pond chart with breathing waves, staggered mirror moment cards with ripple entrance.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Dashboard.tsx
git commit -m "feat: update Dashboard with PondChart and ripple mirror moments"
```

---

### Task 14: Update Trends page with PondChart and ActivityPond

**Files:**
- Modify: `frontend/src/pages/Trends.tsx`

- [ ] **Step 1: Replace Trends.tsx**

Keep the bar chart and pie chart from recharts but re-theme them. Replace the AreaChart with PondChart. Add ActivityPond.

```tsx
import { useEffect, useState } from 'react'
import { getWeeklyTrends, getMonthlyTrends, getTimeSaved } from '../api/trends'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'
import { PondChart } from '../components/PondChart'
import { ActivityPond } from '../components/ActivityPond'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { getDomain } from '../data/domains'

const ACTIVITY_COLORS = ['#5C7A52', '#B05F3F', '#8B4830', '#1F4E68']

export function Trends() {
  const [weekly, setWeekly] = useState<WeeklyTrend[]>([])
  const [timeSaved, setTimeSaved] = useState<TimeSaved | null>(null)
  const [view, setView] = useState<'weekly' | 'monthly'>('weekly')
  const [monthly, setMonthly] = useState<MonthlyTrend[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getWeeklyTrends(), getMonthlyTrends(), getTimeSaved()])
      .then(([w, m, ts]) => { setWeekly(w); setMonthly(m); setTimeSaved(ts) })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16" style={{ color: '#5C7A52' }}>Loading trends...</div>

  const domainData = timeSaved ? Object.entries(timeSaved.byDomain).map(([domain, saved]) => ({
    name: getDomain(domain).name,
    value: saved,
    color: getDomain(domain).color,
  })) : []

  const activityData = (() => {
    if (!timeSaved) return []
    const entries = Object.entries(timeSaved.byActivity).map(([activity, saved]) => ({
      name: activity,
      value: saved,
      breakdown: undefined as { name: string; value: number }[] | undefined,
    }))
    const total = entries.reduce((sum, e) => sum + e.value, 0)
    if (total === 0) return entries
    const significant = entries.filter(e => (e.value / total) >= 0.01)
    const others = entries.filter(e => (e.value / total) < 0.01)
    const othersValue = others.reduce((sum, e) => sum + e.value, 0)
    if (othersValue > 0) significant.push({ name: 'Others', value: othersValue, breakdown: others })
    return significant
  })()

  const trendData = view === 'weekly' ? weekly : monthly.map(m => ({
    week: m.month, domain: m.domain, totalSeconds: m.totalSeconds, totalSaved: m.totalSaved
  }))

  return (
    <div className="space-y-8">
      <div className="flex gap-2">
        {(['weekly', 'monthly'] as const).map(v => (
          <button key={v} onClick={() => setView(v)}
            className="px-4 py-1.5 rounded-full text-sm capitalize"
            style={{
              background: view === v ? 'rgba(92,122,82,0.15)' : 'transparent',
              color: view === v ? '#c8dfc0' : '#6a9a5a',
              border: `1px solid ${view === v ? 'rgba(92,122,82,0.4)' : 'rgba(92,122,82,0.2)'}`,
            }}>
            {v}
          </button>
        ))}
      </div>

      <PondChart data={trendData} />

      <ActivityPond />

      {domainData.length > 0 && (
        <div className="pond-card">
          <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1px' }}>
            Time saved by tool
          </p>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={domainData} layout="vertical">
              <XAxis type="number" unit=" min" tick={{ fill: '#5C7A52', fontSize: 11 }} axisLine={{ stroke: 'rgba(92,122,82,0.15)' }} />
              <YAxis type="category" dataKey="name" width={100} tick={{ fill: '#6a9a5a', fontSize: 12 }} axisLine={false} />
              <Tooltip contentStyle={{ background: 'rgba(15,26,15,0.9)', border: '1px solid rgba(92,122,82,0.3)', borderRadius: 12, color: '#c8dfc0' }} />
              <Bar dataKey="value" name="Minutes saved" radius={[0, 4, 4, 0]}>
                {domainData.map((d, i) => <Cell key={i} fill={d.color} opacity={0.8} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {activityData.length > 0 && (
        <div className="pond-card">
          <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1px' }}>
            What you use AI for
          </p>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={activityData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90}
                label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}>
                {activityData.map((_, i) => <Cell key={i} fill={ACTIVITY_COLORS[i % ACTIVITY_COLORS.length]} opacity={0.8} />)}
              </Pie>
              <Tooltip contentStyle={{ background: 'rgba(15,26,15,0.9)', border: '1px solid rgba(92,122,82,0.3)', borderRadius: 12, color: '#c8dfc0' }}
                content={({ active, payload }) => {
                  if (!active || !payload?.[0]) return null
                  const data = payload[0].payload
                  return (
                    <div style={{ background: 'rgba(15,26,15,0.9)', border: '1px solid rgba(92,122,82,0.3)', borderRadius: 12, padding: '8px 12px', color: '#c8dfc0', fontSize: 13 }}>
                      <p className="font-medium">{data.name}: {data.value} min</p>
                      {data.breakdown?.map((b: { name: string; value: number }) => (
                        <p key={b.name} style={{ color: '#6a9a5a' }}>{b.name}: {b.value} min</p>
                      ))}
                    </div>
                  )
                }} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Verify in browser**

Open Trends page. Should show: weekly/monthly toggle pills, pond chart, activity pond heatmap, dark-themed bar chart and pie chart.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Trends.tsx
git commit -m "feat: update Trends with PondChart, ActivityPond, dark recharts"
```

---

### Task 15: Update Mirror page with staggered entrance

**Files:**
- Modify: `frontend/src/pages/Mirror.tsx`

- [ ] **Step 1: Replace Mirror.tsx**

```tsx
import { useEffect, useState } from 'react'
import { getMirrorMoments } from '../api/insights'
import type { MirrorMoment } from '../types'
import { MirrorMomentCard } from '../components/MirrorMomentCard'

export function Mirror() {
  const [moments, setMoments] = useState<MirrorMoment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMirrorMoments()
      .then(setMoments)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16" style={{ color: '#5C7A52' }}>Loading insights...</div>

  if (moments.length === 0) {
    return (
      <div className="text-center py-16">
        <p style={{ color: '#6a9a5a' }}>Not enough data yet for mirror moments.</p>
        <p className="text-sm mt-2" style={{ color: '#5C7A52' }}>Keep using AI tools and check back soon.</p>
      </div>
    )
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-6" style={{ color: '#c8dfc0' }}>Mirror Moments</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {moments.map((m, i) => <MirrorMomentCard key={i} moment={m} index={i} />)}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/Mirror.tsx
git commit -m "feat: update Mirror page with staggered ripple card entrance"
```

---

### Task 16: Restyle Login page for pond theme

**Files:**
- Modify: `frontend/src/pages/Login.tsx`

- [ ] **Step 1: Replace Login.tsx**

```tsx
import { useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { sendMagicLink } from '../api/auth'
import { useAuth } from '../hooks/useAuth'

export function Login() {
  const { user, loading } = useAuth()
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState(searchParams.get('error') || '')
  const [submitting, setSubmitting] = useState(false)

  if (loading) return <div className="min-h-screen flex items-center justify-center" style={{ background: '#0f1a0f', color: '#5C7A52' }}>Loading...</div>
  if (user) return <Navigate to="/" replace />

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await sendMagicLink(email)
      setSent(true)
    } catch {
      setError('Failed to send magic link. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center" style={{ background: '#0f1a0f' }}>
      <div className="max-w-sm w-full space-y-8 p-8 rounded-2xl"
        style={{ background: 'rgba(92,122,82,0.06)', border: '1px solid rgba(92,122,82,0.15)' }}>
        <div className="text-center">
          <div className="flex justify-center mb-3">
            <svg width="40" height="40" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="3.2" fill="#8fb87a"/>
              <circle cx="16" cy="16" r="8.7" stroke="#8fb87a" strokeWidth="1.8" opacity=".55"/>
              <circle cx="16" cy="16" r="14" stroke="#8fb87a" strokeWidth="1.5" opacity=".25"/>
            </svg>
          </div>
          <h1 className="text-3xl font-bold font-serif" style={{ color: '#c8dfc0' }}>rippl</h1>
          <p className="mt-2 text-sm" style={{ color: '#6a9a5a' }}>Sign in to your dashboard</p>
        </div>

        {error && (
          <div className="p-3 rounded-xl text-sm" style={{ background: 'rgba(248,113,113,0.1)', color: '#f87171' }}>
            {error === 'invalid_token' ? 'That link has expired or already been used.' : error}
          </div>
        )}

        {sent ? (
          <div className="p-4 rounded-xl text-center" style={{ background: 'rgba(92,122,82,0.1)', color: '#8fb87a' }}>
            <p className="font-medium">Check your email</p>
            <p className="text-sm mt-1" style={{ color: '#6a9a5a' }}>We sent a sign-in link to {email}</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              className="w-full px-3 py-2 rounded-xl text-sm focus:outline-none focus:ring-2"
              style={{
                background: 'rgba(92,122,82,0.1)',
                border: '1px solid rgba(92,122,82,0.2)',
                color: '#c8dfc0',
                caretColor: '#8fb87a',
              }}
            />
            <button
              type="submit"
              disabled={submitting}
              className="w-full py-2 rounded-xl text-sm disabled:opacity-50"
              style={{ background: '#5C7A52', color: '#0f1a0f', fontWeight: 500 }}
            >
              {submitting ? 'Sending...' : 'Send magic link'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/Login.tsx
git commit -m "feat: restyle Login page for pond theme"
```

---

### Task 17: Restyle Settings page for pond theme

**Files:**
- Modify: `frontend/src/pages/Settings.tsx`

- [ ] **Step 1: Update Settings.tsx inline styles**

The Settings page uses Tailwind classes that reference the CSS tokens. Most of the styling will come from the updated tokens automatically. The main changes needed:

- Replace `bg-accent` backgrounds on the token display section with inline pond styles
- Replace `bg-error` on the delete confirmation with inline pond styles
- Replace button styles to match pond theme

Read the current file, then replace specific className strings:

In the token display section, replace:
```
className="bg-accent p-4 rounded-card space-y-2"
```
with:
```
className="p-4 rounded-2xl space-y-2" style={{ background: 'rgba(92,122,82,0.1)', border: '1px solid rgba(92,122,82,0.2)' }}
```

In the delete confirmation, replace:
```
className="bg-error p-4 rounded-card space-y-3"
```
with:
```
className="p-4 rounded-2xl space-y-3" style={{ background: 'rgba(248,113,113,0.08)', border: '1px solid rgba(248,113,113,0.2)' }}
```

Replace all `bg-primary text-fg-on-primary rounded-card` buttons with:
```
className="..." style={{ background: '#5C7A52', color: '#0f1a0f' }}
```

Replace all `border-dashed border-default rounded-card` with pond-card style dashed borders:
```
style={{ border: '2px dashed rgba(92,122,82,0.2)', borderRadius: '16px' }}
```

- [ ] **Step 2: Verify Settings page in browser**

Open Settings. Check: collector cards look correct, token display area is pond-themed, delete confirmation is styled, buttons match.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Settings.tsx
git commit -m "feat: restyle Settings page for pond theme"
```

---

### Task 18: Remove old TrendChart component

**Files:**
- Delete: `frontend/src/components/TrendChart.tsx`
- Modify: any files that import it (already replaced in Tasks 13-14)

- [ ] **Step 1: Delete TrendChart.tsx**

```bash
rm frontend/src/components/TrendChart.tsx
```

- [ ] **Step 2: Verify no remaining imports**

```bash
grep -r "TrendChart" frontend/src --include="*.tsx" --include="*.ts"
```

Should return no results (already replaced with PondChart in Dashboard and Trends).

- [ ] **Step 3: Commit**

```bash
git add -A frontend/src/components/TrendChart.tsx
git commit -m "chore: remove old TrendChart recharts component"
```

---

### Task 19: Build the frontend dist and verify

**Files:**
- Modify: `frontend/dist/` (rebuilt)

- [ ] **Step 1: Run production build**

```bash
cd frontend && npm run build
```

Should complete without errors.

- [ ] **Step 2: Start dev server and full visual test**

```bash
cd frontend && npm run dev
```

Checklist:
- [ ] Login page: dark background, pond card, rippl icon, green input
- [ ] Dashboard: hero stat, pond chart with breathing waves, mirror moment cards with stagger
- [ ] Trends: weekly/monthly toggle, pond chart, activity heatmap, dark bar chart, dark pie chart
- [ ] Mirror: staggered ripple card entrance
- [ ] Settings: pond-themed collector cards, token display, delete confirmation
- [ ] Nav: dark with blur, green rippl icon, sync indicator with pulsing dot
- [ ] Ambient background: random ripple rings across the whole page
- [ ] Click anywhere: moss-green concentric rings from cursor
- [ ] Pond chart hover: tooltip appears, single ripple at data point per week
- [ ] Heatmap hover: cell scales up with ripple ring

- [ ] **Step 3: Commit built dist**

```bash
git add frontend/dist
git commit -m "chore: rebuild frontend dist with pond design"
```

---

### Task 20: Final commit and cleanup

- [ ] **Step 1: Remove prototype files (optional, keep for reference)**

The prototype files in `docs/` can stay for design reference or be removed:
- `docs/style-comparison.html`
- `docs/rippl-pond-prototype.html`

- [ ] **Step 2: Final commit**

```bash
git add -A
git commit -m "feat: complete pond design frontend redesign

Dark organic theme with ambient pond background, canvas-based
trend chart with breathing waves and hover ripples, activity
heatmap, framer-motion mirror moment cards, global click ripple,
and sync indicator. The dashboard is the pond."
```

---

## Future Work (Not in this plan)

- **Activity Pond with real data:** Backend endpoint for `SUM(time_saved_minutes) GROUP BY dow, hour` to replace mock heatmap data
- **Number counter animation:** countup.js or framer-motion spring for hero stat
- **Page transitions:** framer-motion `AnimatePresence` with ripple wipe between routes
- **Data sync ripple burst:** When new sync data arrives, trigger a ripple animation from the sync indicator outward across the page
