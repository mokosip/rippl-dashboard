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
