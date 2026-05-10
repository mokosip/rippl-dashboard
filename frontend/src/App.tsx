import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import { Layout } from './components/Layout'
import { ProfileOnboarding } from './components/ProfileOnboarding'
import { useAuth } from './hooks/useAuth'
import { Login } from './pages/Login'
import { Dashboard } from './pages/Dashboard'
import { Trends } from './pages/Trends'
import { Mirror } from './pages/Mirror'
import { Settings } from './pages/Settings'

function OnboardingGate() {
  const { hasProfile, markProfileComplete } = useAuth()

  if (hasProfile === false) {
    return (
      <div className="min-h-screen bg-page">
        <ProfileOnboarding onComplete={markProfileComplete} />
      </div>
    )
  }

  return <Layout />
}

export default function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route element={<OnboardingGate />}>
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
