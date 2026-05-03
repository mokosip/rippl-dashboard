# rippl-dashboard CI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align rippl-dashboard frontend with rippl-web's warm earthy CI — colors, typography, radii, shadows — and add dark mode.

**Architecture:** CSS variables define brand tokens (layer 1) and semantic tokens (layer 2). Dark mode (layer 3) swaps semantic values via `.dark` class. Tailwind v4 `@theme inline` with namespaced registration exposes semantic tokens as utilities. ThemeProvider context manages toggle + localStorage persistence.

**Tech Stack:** Tailwind CSS v4, React 18, Vite, CSS custom properties, Google Fonts (Inter, Source Serif 4)

**Spec:** `docs/superpowers/specs/2026-05-03-rippl-ci-redesign-design.md`

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `frontend/index.html` | Modify | Google Fonts links, theme flash prevention script, title |
| `frontend/src/index.css` | Rewrite | Full theme system: brand tokens, semantic tokens, dark mode, `@theme inline` |
| `frontend/src/context/ThemeContext.tsx` | Create | ThemeProvider context + `useTheme` hook |
| `frontend/src/App.tsx` | Modify | Wrap with ThemeProvider |
| `frontend/src/components/Layout.tsx` | Modify | Nav restyled, dark mode toggle, serif logo |
| `frontend/src/pages/Login.tsx` | Modify | Color class swap |
| `frontend/src/pages/Dashboard.tsx` | Modify | Color class swap |
| `frontend/src/pages/Trends.tsx` | Modify | Color class swap + pie chart warm-shift |
| `frontend/src/pages/Mirror.tsx` | Modify | Color class swap |
| `frontend/src/pages/Settings.tsx` | Modify | Color class swap |
| `frontend/src/components/TimeSavedCard.tsx` | Modify | Color class swap |
| `frontend/src/components/TrendChart.tsx` | Modify | Color class swap |
| `frontend/src/components/MirrorMomentCard.tsx` | Modify | Color class swap |
| `frontend/src/components/CollectorCard.tsx` | Modify | Color class swap |
| `frontend/src/components/OnboardingChecklist.tsx` | Modify | Color class swap + accent colors |

---

### Task 1: Theme Foundation — `index.css` + `index.html`

**Files:**
- Rewrite: `frontend/src/index.css`
- Modify: `frontend/index.html`

- [ ] **Step 1: Rewrite `frontend/src/index.css` with full theme system**

Replace the entire file with:

```css
@import "tailwindcss";

/* ── Layer 1: Brand Tokens (from rippl-web) ── */
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

/* ── Layer 2: Semantic Tokens (Light) ── */
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

/* ── Layer 3: Dark Mode ── */
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

/* ── Tailwind Exposure ── */
@theme inline {
  --background-page: var(--bg-page);
  --background-card: var(--bg-card);
  --background-nav: var(--bg-nav);
  --background-input: var(--bg-input);
  --background-muted: var(--bg-muted);
  --background-accent: var(--bg-accent);
  --background-error: var(--bg-error);
  --background-success: var(--bg-success);

  --text-fg: var(--fg);
  --text-fg-secondary: var(--fg-secondary);
  --text-fg-muted: var(--fg-muted);
  --text-fg-on-primary: var(--fg-on-primary);
  --text-fg-error: var(--fg-error);
  --text-fg-success: var(--fg-success);
  --text-fg-active: var(--fg-active);

  --color-primary: var(--primary);
  --color-primary-hover: var(--primary-hover);
  --color-secondary: var(--secondary);
  --color-destructive: var(--destructive);
  --color-ring: var(--ring);

  --border-color-default: var(--border-default);
  --border-color-subtle: var(--border-subtle);

  --radius-card: var(--radius-card);
  --radius-input: var(--radius-input);
  --radius-pill: var(--radius-pill);

  --font-sans: var(--font-sans);
  --font-serif: var(--font-serif);
}
```

- [ ] **Step 2: Add Google Fonts + flash prevention + title to `frontend/index.html`**

Replace the entire `<head>` content:

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>rippl dashboard</title>
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Source+Serif+4:wght@400;500&display=swap" rel="stylesheet" />
    <script>
      (function(){
        var t = localStorage.getItem('theme');
        if (t === 'dark' || (!t && matchMedia('(prefers-color-scheme:dark)').matches))
          document.documentElement.classList.add('dark');
      })()
    </script>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 3: Verify Tailwind picks up new tokens**

Run: `cd frontend && npx vite build 2>&1 | head -20`

Expected: Build succeeds with no CSS errors. If `@theme inline` syntax isn't recognized, check Tailwind version is 4.2+.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/index.css frontend/index.html
git commit -m "feat: add rippl CI theme tokens, dark mode variables, and @theme inline exposure"
```

---

### Task 2: ThemeProvider Context

**Files:**
- Create: `frontend/src/context/ThemeContext.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Create `frontend/src/context/ThemeContext.tsx`**

```tsx
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

type Theme = 'light' | 'dark' | 'system'

interface ThemeState {
  theme: Theme
  resolved: 'light' | 'dark'
  setTheme: (t: Theme) => void
}

const ThemeContext = createContext<ThemeState>({
  theme: 'system',
  resolved: 'light',
  setTheme: () => {},
})

export function useTheme() {
  return useContext(ThemeContext)
}

function getResolved(theme: Theme): 'light' | 'dark' {
  if (theme !== 'system') return theme
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    const stored = localStorage.getItem('theme')
    if (stored === 'light' || stored === 'dark') return stored
    return 'system'
  })
  const [resolved, setResolved] = useState<'light' | 'dark'>(() => getResolved(theme))

  const setTheme = (t: Theme) => {
    setThemeState(t)
    if (t === 'system') {
      localStorage.removeItem('theme')
    } else {
      localStorage.setItem('theme', t)
    }
  }

  useEffect(() => {
    const r = getResolved(theme)
    setResolved(r)
    document.documentElement.classList.toggle('dark', r === 'dark')
  }, [theme])

  useEffect(() => {
    if (theme !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const handler = () => {
      const r = getResolved('system')
      setResolved(r)
      document.documentElement.classList.toggle('dark', r === 'dark')
    }
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [theme])

  return (
    <ThemeContext.Provider value={{ theme, resolved, setTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}
```

- [ ] **Step 2: Wrap App with ThemeProvider in `frontend/src/App.tsx`**

Replace the file with:

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import { Layout } from './components/Layout'
import { Login } from './pages/Login'
import { Dashboard } from './pages/Dashboard'
import { Trends } from './pages/Trends'
import { Mirror } from './pages/Mirror'
import { Settings } from './pages/Settings'

export default function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route element={<Layout />}>
              <Route path="/" element={<Dashboard />} />
              <Route path="/trends" element={<Trends />} />
              <Route path="/mirror" element={<Mirror />} />
              <Route path="/settings" element={<Settings />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  )
}
```

- [ ] **Step 3: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit 2>&1 | head -20`

Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/context/ThemeContext.tsx frontend/src/App.tsx
git commit -m "feat: add ThemeProvider with localStorage persistence and system preference fallback"
```

---

### Task 3: Layout + Nav Restyle + Dark Mode Toggle

**Files:**
- Modify: `frontend/src/components/Layout.tsx`

- [ ] **Step 1: Replace `frontend/src/components/Layout.tsx`**

```tsx
import { NavLink, Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useTheme } from '../context/ThemeContext'

export function Layout() {
  const { user, loading, logout } = useAuth()
  const { resolved, setTheme } = useTheme()

  if (loading) return <div className="min-h-screen flex items-center justify-center bg-page text-fg-muted">Loading...</div>
  if (!user) return <Navigate to="/login" replace />

  const toggleTheme = () => setTheme(resolved === 'dark' ? 'light' : 'dark')

  return (
    <div className="min-h-screen bg-page">
      <nav className="bg-nav border-b border-default">
        <div className="max-w-5xl mx-auto px-4 py-3 flex justify-between items-center">
          <NavLink to="/" className="text-xl font-bold font-serif text-fg">rippl</NavLink>
          <div className="flex gap-6 items-center">
            <NavLink to="/" end className={({ isActive }) =>
              isActive ? 'text-fg-active font-medium' : 'text-fg-secondary hover:text-fg'
            }>Dashboard</NavLink>
            <NavLink to="/trends" className={({ isActive }) =>
              isActive ? 'text-fg-active font-medium' : 'text-fg-secondary hover:text-fg'
            }>Trends</NavLink>
            <NavLink to="/mirror" className={({ isActive }) =>
              isActive ? 'text-fg-active font-medium' : 'text-fg-secondary hover:text-fg'
            }>Mirror</NavLink>
            <NavLink to="/settings" className={({ isActive }) =>
              isActive ? 'text-fg-active font-medium' : 'text-fg-secondary hover:text-fg'
            }>Settings</NavLink>
            <button onClick={toggleTheme} className="text-fg-muted hover:text-fg text-sm" aria-label="Toggle theme">
              {resolved === 'dark' ? '☀️' : '🌙'}
            </button>
            <button onClick={logout} className="text-fg-muted hover:text-fg-secondary text-sm">
              Sign out
            </button>
          </div>
        </div>
      </nav>
      <main className="max-w-5xl mx-auto px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/Layout.tsx
git commit -m "feat: restyle nav with rippl CI tokens, add dark mode toggle"
```

---

### Task 4: Login Page Restyle

**Files:**
- Modify: `frontend/src/pages/Login.tsx`

- [ ] **Step 1: Replace `frontend/src/pages/Login.tsx`**

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

  if (loading) return <div className="min-h-screen flex items-center justify-center bg-page text-fg-muted">Loading...</div>
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
    <div className="min-h-screen flex items-center justify-center bg-page">
      <div className="max-w-sm w-full space-y-8 p-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold font-serif text-fg">rippl</h1>
          <p className="mt-2 text-fg-secondary">Sign in to your dashboard</p>
        </div>

        {error && (
          <div className="bg-error text-fg-error p-3 rounded-card text-sm">
            {error === 'invalid_token' ? 'That link has expired or already been used.' : error}
          </div>
        )}

        {sent ? (
          <div className="bg-success text-fg-success p-4 rounded-card text-center">
            <p className="font-medium">Check your email</p>
            <p className="text-sm mt-1">We sent a sign-in link to {email}</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              className="w-full px-3 py-2 border border-default rounded-input bg-input text-fg focus:outline-none focus:ring-2 focus:ring-ring"
            />
            <button
              type="submit"
              disabled={submitting}
              className="w-full py-2 bg-primary text-fg-on-primary rounded-input hover:bg-primary-hover disabled:opacity-50"
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
git commit -m "feat: restyle login page with rippl CI tokens"
```

---

### Task 5: Dashboard + TimeSavedCard + MirrorMomentCard + OnboardingChecklist

**Files:**
- Modify: `frontend/src/pages/Dashboard.tsx`
- Modify: `frontend/src/components/TimeSavedCard.tsx`
- Modify: `frontend/src/components/MirrorMomentCard.tsx`
- Modify: `frontend/src/components/OnboardingChecklist.tsx`

- [ ] **Step 1: Replace `frontend/src/pages/Dashboard.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { getWeeklyTrends, getTimeSaved } from '../api/trends'
import { getMirrorMoments } from '../api/insights'
import type { WeeklyTrend, TimeSaved, MirrorMoment } from '../types'
import { TimeSavedCard } from '../components/TimeSavedCard'
import { TrendChart } from '../components/TrendChart'
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

  if (loading) return <div className="text-center py-16 text-fg-muted">Loading dashboard...</div>
  if (!timeSaved || timeSaved.total === 0) return <OnboardingChecklist />

  return (
    <div className="space-y-8">
      <TimeSavedCard data={timeSaved} />
      <TrendChart data={trends} />
      {insights.length > 0 && (
        <div>
          <h3 className="text-sm text-fg-muted uppercase tracking-wide mb-4">Mirror Moments</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {insights.map((m, i) => <MirrorMomentCard key={i} moment={m} />)}
          </div>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Replace `frontend/src/components/TimeSavedCard.tsx`**

```tsx
import type { TimeSaved } from '../types'
import { getComparison } from '../data/comparisons'

export function TimeSavedCard({ data }: { data: TimeSaved }) {
  const hours = Math.floor(data.total / 60)
  const minutes = data.total % 60

  return (
    <div className="bg-card rounded-card shadow-sm p-6">
      <p className="text-sm text-fg-muted uppercase tracking-wide">Total time saved</p>
      <p className="text-4xl font-bold text-fg mt-1">
        {hours > 0 && <>{hours}h </>}{minutes}m
      </p>
      <p className="text-fg-muted mt-2">That's {getComparison(data.total)}</p>
    </div>
  )
}
```

- [ ] **Step 3: Replace `frontend/src/components/MirrorMomentCard.tsx`**

```tsx
import type { MirrorMoment } from '../types'

const TYPE_ICONS: Record<string, string> = {
  weekly_usage: '📈',
  top_tool: '🎯',
  time_saving_activity: '⏱️',
  busiest_day: '📅',
}

export function MirrorMomentCard({ moment }: { moment: MirrorMoment }) {
  return (
    <div className="bg-card rounded-card shadow-sm p-4">
      <span className="text-2xl">{TYPE_ICONS[moment.type] ?? '💡'}</span>
      <p className="text-fg-secondary mt-2 text-sm">{moment.message}</p>
    </div>
  )
}
```

- [ ] **Step 4: Replace `frontend/src/components/OnboardingChecklist.tsx`**

```tsx
export function OnboardingChecklist() {
  return (
    <div className="max-w-md mx-auto mt-16 text-center">
      <h2 className="text-2xl font-bold text-fg">Welcome to rippl</h2>
      <p className="text-fg-muted mt-2">Let's get you set up</p>
      <div className="mt-8 space-y-4 text-left">
        <Step number={1} title="Connect a data source" description="Install the Chrome extension to start tracking your AI usage." />
        <Step number={2} title="Use AI for a day" description="Browse your favorite AI tools. We'll capture your sessions automatically." />
        <Step number={3} title="See your insights" description="Come back here to explore trends, time saved, and mirror moments." />
      </div>
      <a
        href="/settings"
        className="inline-block mt-8 px-6 py-2 bg-primary text-fg-on-primary rounded-input hover:bg-primary-hover"
      >
        Go to Settings to connect
      </a>
    </div>
  )
}

function Step({ number, title, description }: { number: number; title: string; description: string }) {
  return (
    <div className="flex gap-4 items-start">
      <div className="w-8 h-8 rounded-full bg-accent text-fg-active flex items-center justify-center font-bold text-sm flex-shrink-0">
        {number}
      </div>
      <div>
        <p className="font-medium text-fg">{title}</p>
        <p className="text-sm text-fg-muted">{description}</p>
      </div>
    </div>
  )
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/Dashboard.tsx frontend/src/components/TimeSavedCard.tsx frontend/src/components/MirrorMomentCard.tsx frontend/src/components/OnboardingChecklist.tsx
git commit -m "feat: restyle dashboard page and cards with rippl CI tokens"
```

---

### Task 6: Trends Page + TrendChart + Pie Chart Warm-Shift

**Files:**
- Modify: `frontend/src/pages/Trends.tsx`
- Modify: `frontend/src/components/TrendChart.tsx`

- [ ] **Step 1: Replace `frontend/src/components/TrendChart.tsx`**

```tsx
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { WeeklyTrend } from '../types'
import { getDomain } from '../data/domains'

interface ChartData {
  week: string
  [domain: string]: number | string
}

export function TrendChart({ data }: { data: WeeklyTrend[] }) {
  const domains = [...new Set(data.map(d => d.domain))]

  const byWeek: Record<string, ChartData> = {}
  for (const d of data) {
    if (!byWeek[d.week]) byWeek[d.week] = { week: d.week }
    byWeek[d.week][d.domain] = Math.round(d.totalSeconds / 60)
  }
  const chartData = Object.values(byWeek).sort((a, b) => a.week.localeCompare(b.week))

  if (chartData.length === 0) return null

  return (
    <div className="bg-card rounded-card shadow-sm p-6">
      <h3 className="text-sm text-fg-muted uppercase tracking-wide mb-4">AI Usage (minutes/week)</h3>
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={chartData}>
          <XAxis dataKey="week" tickFormatter={w => new Date(w).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} />
          <YAxis />
          <Tooltip />
          {domains.map(domain => (
            <Area
              key={domain}
              type="monotone"
              dataKey={domain}
              stackId="1"
              fill={getDomain(domain).color}
              stroke={getDomain(domain).color}
              name={getDomain(domain).name}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
```

- [ ] **Step 2: Replace `frontend/src/pages/Trends.tsx`**

Note the warm-shifted activity chart colors: `ACTIVITY_COLORS` array replaces the old indigo/emerald/amber/red.

```tsx
import { useEffect, useState } from 'react'
import { getWeeklyTrends, getMonthlyTrends, getTimeSaved } from '../api/trends'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'
import { TrendChart } from '../components/TrendChart'
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

  if (loading) return <div className="text-center py-16 text-fg-muted">Loading trends...</div>

  const domainData = timeSaved ? Object.entries(timeSaved.byDomain).map(([domain, saved]) => ({
    name: getDomain(domain).name,
    value: saved,
    color: getDomain(domain).color,
  })) : []

  const activityData = timeSaved ? Object.entries(timeSaved.byActivity).map(([activity, saved]) => ({
    name: activity,
    value: saved,
  })) : []

  return (
    <div className="space-y-8">
      <div className="flex gap-2">
        <button onClick={() => setView('weekly')} className={`px-3 py-1 rounded-input text-sm ${view === 'weekly' ? 'bg-primary text-fg-on-primary' : 'bg-muted text-fg-secondary'}`}>Weekly</button>
        <button onClick={() => setView('monthly')} className={`px-3 py-1 rounded-input text-sm ${view === 'monthly' ? 'bg-primary text-fg-on-primary' : 'bg-muted text-fg-secondary'}`}>Monthly</button>
      </div>

      <TrendChart data={view === 'weekly' ? weekly : monthly.map(m => ({ week: m.month, domain: m.domain, totalSeconds: m.totalSeconds, totalSaved: m.totalSaved }))} />

      {domainData.length > 0 && (
        <div className="bg-card rounded-card shadow-sm p-6">
          <h3 className="text-sm text-fg-muted uppercase tracking-wide mb-4">Time saved by tool</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={domainData} layout="vertical">
              <XAxis type="number" unit=" min" />
              <YAxis type="category" dataKey="name" width={100} />
              <Tooltip />
              <Bar dataKey="value" name="Minutes saved">
                {domainData.map((d, i) => <Cell key={i} fill={d.color} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {activityData.length > 0 && (
        <div className="bg-card rounded-card shadow-sm p-6">
          <h3 className="text-sm text-fg-muted uppercase tracking-wide mb-4">What you use AI for</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={activityData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}>
                {activityData.map((_, i) => <Cell key={i} fill={ACTIVITY_COLORS[i % ACTIVITY_COLORS.length]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Trends.tsx frontend/src/components/TrendChart.tsx
git commit -m "feat: restyle trends page with rippl CI tokens and warm-shifted chart palette"
```

---

### Task 7: Mirror + Settings + CollectorCard

**Files:**
- Modify: `frontend/src/pages/Mirror.tsx`
- Modify: `frontend/src/pages/Settings.tsx`
- Modify: `frontend/src/components/CollectorCard.tsx`

- [ ] **Step 1: Replace `frontend/src/pages/Mirror.tsx`**

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

  if (loading) return <div className="text-center py-16 text-fg-muted">Loading insights...</div>

  if (moments.length === 0) {
    return (
      <div className="text-center py-16">
        <p className="text-fg-muted">Not enough data yet for mirror moments.</p>
        <p className="text-fg-muted text-sm mt-2">Keep using AI tools and check back soon.</p>
      </div>
    )
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-fg mb-6">Mirror Moments</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {moments.map((m, i) => <MirrorMomentCard key={i} moment={m} />)}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Replace `frontend/src/components/CollectorCard.tsx`**

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
    <div className="flex items-center justify-between bg-card rounded-card shadow-sm p-4">
      <div>
        <p className="font-medium text-fg">{TYPE_LABELS[collector.type] ?? collector.type}</p>
        <p className="text-sm text-fg-muted">
          Connected {new Date(collector.linkedAt).toLocaleDateString()}
        </p>
      </div>
      <button
        onClick={() => onRemove(collector.id)}
        className="text-fg-error hover:text-destructive text-sm"
      >
        Remove
      </button>
    </div>
  )
}
```

- [ ] **Step 3: Replace `frontend/src/pages/Settings.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { getCollectors, addCollector, removeCollector } from '../api/collectors'
import { deleteAccount } from '../api/account'
import type { CollectorInfo } from '../types'
import { CollectorCard } from '../components/CollectorCard'
import { useAuth } from '../hooks/useAuth'

export function Settings() {
  const { user } = useAuth()
  const [collectors, setCollectors] = useState<CollectorInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  useEffect(() => {
    getCollectors().then(setCollectors).finally(() => setLoading(false))
  }, [])

  const handleAddExtension = async () => {
    const c = await addCollector('chrome_extension')
    setCollectors(prev => [...prev, c])
  }

  const handleRemove = async (id: string) => {
    await removeCollector(id)
    setCollectors(prev => prev.filter(c => c.id !== id))
  }

  const handleDelete = async () => {
    await deleteAccount()
    window.location.href = '/login'
  }

  return (
    <div className="space-y-10">
      <section>
        <h2 className="text-lg font-semibold text-fg mb-4">Data Sources</h2>
        {loading ? (
          <p className="text-fg-muted">Loading...</p>
        ) : (
          <div className="space-y-3">
            {collectors.map(c => (
              <CollectorCard key={c.id} collector={c} onRemove={handleRemove} />
            ))}
            {!collectors.some(c => c.type === 'chrome_extension') && (
              <button
                onClick={handleAddExtension}
                className="w-full py-3 border-2 border-dashed border-default rounded-card text-fg-muted hover:border-ring hover:text-fg-active"
              >
                + Connect Chrome Extension
              </button>
            )}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-fg mb-2">Account</h2>
        <p className="text-sm text-fg-muted mb-2">Signed in as {user?.email}</p>
        {showDeleteConfirm ? (
          <div className="bg-error p-4 rounded-card space-y-3">
            <p className="text-fg-error text-sm font-medium">
              This will permanently delete your account and all data. This cannot be undone.
            </p>
            <div className="flex gap-2">
              <button onClick={handleDelete} className="px-4 py-2 bg-destructive text-fg-on-primary rounded-input text-sm hover:opacity-90">
                Yes, delete everything
              </button>
              <button onClick={() => setShowDeleteConfirm(false)} className="px-4 py-2 bg-muted rounded-input text-sm text-fg-secondary">
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <button
            onClick={() => setShowDeleteConfirm(true)}
            className="text-fg-error hover:text-destructive text-sm"
          >
            Delete my account
          </button>
        )}
      </section>
    </div>
  )
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/Mirror.tsx frontend/src/pages/Settings.tsx frontend/src/components/CollectorCard.tsx
git commit -m "feat: restyle mirror, settings, and collector card with rippl CI tokens"
```

---

### Task 8: Visual Verification

**Files:** None (verification only)

- [ ] **Step 1: Start dev server**

Run: `cd frontend && npm run dev`

Expected: Server starts at http://localhost:5173

- [ ] **Step 2: Check light mode**

Open http://localhost:5173 in browser. Verify:
- Page background is warm paper (#EFEAE0), not white or gray
- Nav background is cream (#FBF8F1) with bottom border
- Logo "rippl" is in serif font (Source Serif 4)
- Primary buttons are moss-dark green (#3F5639)
- Active nav link is moss-dark green
- Cards are white with subtle shadow
- Text hierarchy: dark ink for headings, medium for body, lighter for labels

- [ ] **Step 3: Check dark mode**

Click moon icon in nav. Verify:
- Page background is warm dark (#1a1814), not cold black
- Cards are slightly lighter dark (#252118)
- Text is cream-colored
- Primary buttons are moss green (#5C7A52)
- Toggle icon changes to sun

- [ ] **Step 4: Check persistence**

Refresh browser in dark mode. Verify dark mode persists (no flash of light mode).

- [ ] **Step 5: Check login page**

Navigate to /login (or sign out). Verify:
- Login page uses paper background
- Input has proper border and focus ring in moss green
- Button is moss-dark green
- Follows OS dark/light preference (no toggle available)

- [ ] **Step 6: Check trends page**

Navigate to /trends. Verify:
- Weekly/Monthly toggle uses primary/muted colors
- Pie chart uses warm palette (moss, terra, terra-dark, ocean-fg)
- Domain bar chart still uses original brand colors per tool
