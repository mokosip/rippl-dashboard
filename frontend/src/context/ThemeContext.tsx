import { createContext, useContext, useEffect, type ReactNode } from 'react'

type Theme = 'dark'

interface ThemeState {
  theme: Theme
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
  useEffect(() => {
    document.documentElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  }, [])

  return (
    <ThemeContext.Provider value={{ theme: 'dark', resolved: 'dark', setTheme: () => {} }}>
      {children}
    </ThemeContext.Provider>
  )
}
