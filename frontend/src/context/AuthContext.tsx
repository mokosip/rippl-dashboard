import { createContext, useEffect, useState, type ReactNode } from 'react'
import type { User } from '../types'
import { getMe, logout as apiLogout } from '../api/auth'
import { getProfile } from '../api/profile'

interface AuthState {
  user: User | null
  loading: boolean
  logout: () => Promise<void>
  refresh: () => Promise<void>
  hasProfile: boolean | null
  markProfileComplete: () => void
}

export const AuthContext = createContext<AuthState>({
  user: null,
  loading: true,
  logout: async () => {},
  refresh: async () => {},
  hasProfile: null,
  markProfileComplete: () => {},
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [hasProfile, setHasProfile] = useState<boolean | null>(null)

  const refresh = async () => {
    try {
      const u = await getMe()
      setUser(u)
      if (u) {
        const p = await getProfile()
        setHasProfile(p !== null)
      }
    } catch {
      setUser(null)
      setHasProfile(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { refresh() }, [])

  const logout = async () => {
    await apiLogout()
    setUser(null)
    setHasProfile(null)
    window.location.href = '/login'
  }

  const markProfileComplete = () => setHasProfile(true)

  return (
    <AuthContext.Provider value={{ user, loading, logout, refresh, hasProfile, markProfileComplete }}>
      {children}
    </AuthContext.Provider>
  )
}
