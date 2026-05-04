import { NavLink, Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { AmbientPond } from './AmbientPond'
import { ClickRipple } from './ClickRipple'
import { SyncIndicator } from './SyncIndicator'

export function Layout() {
  const { user, loading, logout } = useAuth()

  if (loading) return <div className="min-h-screen flex items-center justify-center bg-page text-fg-muted">Loading...</div>
  if (!user) return <Navigate to="/login" replace />

  return (
    <div className="min-h-screen bg-page">
      <AmbientPond />
      <ClickRipple />
      <nav className="fixed top-0 left-0 right-0 z-50 border-b border-default"
        style={{ background: 'var(--bg-nav-blur)', backdropFilter: 'blur(16px)' }}>
        <div className="max-w-5xl mx-auto px-4 py-3 flex justify-between items-center">
          <NavLink to="/" className="flex items-center gap-2 text-xl font-bold font-serif text-fg">
            <span style={{ color: 'var(--fg-accent)' }}>
              <svg width="22" height="22" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="16" cy="16" r="3.2" fill="currentColor"/>
                <circle cx="16" cy="16" r="8.7" stroke="currentColor" strokeWidth="1.8" opacity=".55"/>
                <circle cx="16" cy="16" r="14" stroke="currentColor" strokeWidth="1.5" opacity=".25"/>
              </svg>
            </span>
            rippl
          </NavLink>
          <div className="flex gap-6 items-center">
            <SyncIndicator />
            <NavLink to="/trends" className={({ isActive }) =>
              `text-sm ${isActive ? 'text-fg font-medium' : 'text-fg-secondary hover:text-fg'}`
            }>Trends</NavLink>
            <NavLink to="/mirror" className={({ isActive }) =>
              `text-sm ${isActive ? 'text-fg font-medium' : 'text-fg-secondary hover:text-fg'}`
            }>Mirror</NavLink>
            <NavLink to="/settings" className={({ isActive }) =>
              `text-sm ${isActive ? 'text-fg font-medium' : 'text-fg-secondary hover:text-fg'}`
            }>Settings</NavLink>
            <button onClick={logout} className="text-fg-muted hover:text-fg-secondary text-sm">
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
