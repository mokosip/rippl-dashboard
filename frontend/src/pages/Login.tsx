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

  if (loading) return <div className="min-h-screen flex items-center justify-center">Loading...</div>
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
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-sm w-full space-y-8 p-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">rippl</h1>
          <p className="mt-2 text-gray-600">Sign in to your dashboard</p>
        </div>

        {error && (
          <div className="bg-red-50 text-red-700 p-3 rounded text-sm">
            {error === 'invalid_token' ? 'That link has expired or already been used.' : error}
          </div>
        )}

        {sent ? (
          <div className="bg-green-50 text-green-700 p-4 rounded text-center">
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
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <button
              type="submit"
              disabled={submitting}
              className="w-full py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50"
            >
              {submitting ? 'Sending...' : 'Send magic link'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
