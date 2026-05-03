import { NavLink, Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function Layout() {
  const { user, loading, logout } = useAuth()

  if (loading) return <div className="min-h-screen flex items-center justify-center">Loading...</div>
  if (!user) return <Navigate to="/login" replace />

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-5xl mx-auto px-4 py-3 flex justify-between items-center">
          <NavLink to="/" className="text-xl font-bold text-gray-900">rippl</NavLink>
          <div className="flex gap-6 items-center">
            <NavLink to="/" end className={({ isActive }) =>
              isActive ? 'text-indigo-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }>Dashboard</NavLink>
            <NavLink to="/trends" className={({ isActive }) =>
              isActive ? 'text-indigo-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }>Trends</NavLink>
            <NavLink to="/mirror" className={({ isActive }) =>
              isActive ? 'text-indigo-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }>Mirror</NavLink>
            <NavLink to="/settings" className={({ isActive }) =>
              isActive ? 'text-indigo-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }>Settings</NavLink>
            <button onClick={logout} className="text-gray-400 hover:text-gray-600 text-sm">
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
