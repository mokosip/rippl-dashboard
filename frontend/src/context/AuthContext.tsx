import { createContext, useEffect, useState, type ReactNode } from 'react'
import type { User } from '../types'
import { getMe, logout as apiLogout } from '../api/auth'

interface AuthState {
  user: User | null
  loading: boolean
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

export const AuthContext = createContext<AuthState>({
  user: null,
  loading: true,
  logout: async () => {},
  refresh: async () => {},
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = async () => {
    try {
      const u = await getMe()
      setUser(u)
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { refresh() }, [])

  const logout = async () => {
    await apiLogout()
    setUser(null)
    window.location.href = '/login'
  }

  return (
    <AuthContext.Provider value={{ user, loading, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}
