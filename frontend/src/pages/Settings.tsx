import { useEffect, useState } from 'react'
import { getCollectors, addCollector, removeCollector } from '../api/collectors'
import { deleteAccount } from '../api/account'
import { getProfile, updateProfile } from '../api/profile'
import type { CollectorInfo, TaskMix } from '../types'
import { CollectorCard } from '../components/CollectorCard'
import { useAuth } from '../hooks/useAuth'
import { useExtension } from '../hooks/useExtension'
import { useTheme } from '../context/ThemeContext'

const TASK_LABELS: Record<keyof TaskMix, string> = {
  writing: 'Writing',
  coding: 'Coding',
  research: 'Research',
  planning: 'Planning',
  communication: 'Communication',
  other: 'Other',
}

const TASK_KEYS = Object.keys(TASK_LABELS) as (keyof TaskMix)[]

export function Settings() {
  const { user } = useAuth()
  const { resolved, setTheme } = useTheme()
  const { status: extStatus, sendToken } = useExtension()
  const [collectors, setCollectors] = useState<CollectorInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [pendingToken, setPendingToken] = useState<string | null>(null)
  const [connecting, setConnecting] = useState(false)
  const [copied, setCopied] = useState(false)
  const [profile, setProfile] = useState<{ task_mix: TaskMix; personal_adjustment_factor: number } | null>(null)
  const [profileLoading, setProfileLoading] = useState(true)
  const [editingProfile, setEditingProfile] = useState(false)
  const [editMix, setEditMix] = useState<TaskMix | null>(null)
  const [profileSaving, setProfileSaving] = useState(false)

  useEffect(() => {
    getCollectors().then(setCollectors).finally(() => setLoading(false))
    getProfile().then(setProfile).finally(() => setProfileLoading(false))
  }, [])

  const handleConnect = async () => {
    setConnecting(true)
    try {
      const c = await addCollector('chrome_extension')
      setCollectors(prev => [...prev, c])
      if (c.token && extStatus === 'installed') {
        const sent = await sendToken(c.token)
        if (!sent) setPendingToken(c.token)
      } else if (c.token) {
        setPendingToken(c.token)
      }
    } finally {
      setConnecting(false)
    }
  }

  const handleCopy = async () => {
    if (!pendingToken) return
    await navigator.clipboard.writeText(pendingToken)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleRemove = async (id: string) => {
    await removeCollector(id)
    setCollectors(prev => prev.filter(c => c.id !== id))
    setPendingToken(null)
  }

  const handleDelete = async () => {
    await deleteAccount()
    window.location.href = '/login'
  }

  const adjustSlider = (key: keyof TaskMix, value: number) => {
    if (!editMix) return
    const clamped = Math.max(0, Math.min(1, value))
    const others = TASK_KEYS.filter(k => k !== key)
    const otherSum = others.reduce((s, k) => s + editMix[k], 0)
    const remaining = 1 - clamped
    const updated = { ...editMix, [key]: clamped }

    if (otherSum > 0) {
      const scale = remaining / otherSum
      others.forEach(k => {
        updated[k] = Math.max(0, editMix[k] * scale)
      })
    } else {
      const even = remaining / others.length
      others.forEach(k => {
        updated[k] = even
      })
    }

    setEditMix(updated)
  }

  const saveProfile = async () => {
    if (!editMix) return
    setProfileSaving(true)
    try {
      const updated = await updateProfile({ task_mix: editMix })
      setProfile(updated)
      setEditingProfile(false)
      setEditMix(null)
    } finally {
      setProfileSaving(false)
    }
  }

  const hasExtension = collectors.some(c => c.type === 'chrome_extension')

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

            {pendingToken && (
              <div className="p-4 rounded-2xl space-y-2 bg-input border border-default">
                <p className="text-sm font-medium text-fg">Extension API Token</p>
                <p className="text-xs text-fg-muted">Copy this token — it won't be shown again.</p>
                <div className="flex gap-2">
                  <code className="flex-1 px-3 py-2 rounded-xl text-xs text-fg-secondary break-all bg-card border border-default">
                    {pendingToken}
                  </code>
                  <button
                    onClick={handleCopy}
                    className="px-3 py-2 bg-primary text-fg-on-primary rounded-xl text-xs hover:bg-primary-hover"
                  >
                    {copied ? 'Copied' : 'Copy'}
                  </button>
                </div>
              </div>
            )}

            {!hasExtension && (
              <div className="space-y-2">
                {extStatus === 'installed' ? (
                  <button
                    onClick={handleConnect}
                    disabled={connecting}
                    className="w-full py-3 bg-primary text-fg-on-primary rounded-2xl hover:bg-primary-hover disabled:opacity-50"
                  >
                    {connecting ? 'Connecting...' : 'Connect Chrome Extension'}
                  </button>
                ) : extStatus === 'not_installed' ? (
                  <div className="space-y-2">
                    <button
                      onClick={handleConnect}
                      disabled={connecting}
                      className="w-full py-3 border-2 border-dashed border-default rounded-2xl text-fg-muted hover:text-fg-active"
                    >
                      {connecting ? 'Connecting...' : '+ Connect Chrome Extension'}
                    </button>
                    <p className="text-xs text-fg-muted text-center">
                      Extension not detected. You'll need to paste the token manually after installing.
                    </p>
                  </div>
                ) : extStatus === 'checking' ? (
                  <p className="text-sm text-fg-muted text-center py-3">Checking for extension...</p>
                ) : (
                  <button
                    onClick={handleConnect}
                    disabled={connecting}
                    className="w-full py-3 border-2 border-dashed border-default rounded-card text-fg-muted hover:border-ring hover:text-fg-active"
                  >
                    {connecting ? 'Connecting...' : '+ Connect Chrome Extension'}
                  </button>
                )}
              </div>
            )}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-fg mb-4">Appearance</h2>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setTheme(resolved === 'dark' ? 'light' : 'dark')}
            className="relative w-14 h-7 rounded-full transition-colors duration-300"
            style={{ background: resolved === 'dark' ? 'var(--primary)' : 'var(--border-default)' }}
            aria-label="Toggle theme"
          >
            <div
              className="absolute top-0.5 w-6 h-6 rounded-full shadow-sm transition-all duration-300 flex items-center justify-center text-xs"
              style={{
                left: resolved === 'dark' ? 'calc(100% - 1.625rem)' : '0.125rem',
                background: 'var(--bg-card)',
              }}
            >
              {resolved === 'dark' ? '🌙' : '☀️'}
            </div>
          </button>
        </div>
      </section>

      <section>
        <h2 className="text-lg font-semibold text-fg mb-4">AI Usage Profile</h2>
        {profileLoading ? (
          <p className="text-fg-muted">Loading...</p>
        ) : editingProfile ? (
          <div className="space-y-3">
            {TASK_KEYS.map(key => (
              <div key={key} className="flex items-center gap-3">
                <span className="w-28 text-sm text-fg-secondary">{TASK_LABELS[key]}</span>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={Math.round((editMix?.[key] ?? 0) * 100)}
                  onChange={e => adjustSlider(key, parseInt(e.target.value, 10) / 100)}
                  className="flex-1 accent-primary"
                />
                <span className="w-10 text-right text-sm text-fg-muted">
                  {Math.round((editMix?.[key] ?? 0) * 100)}%
                </span>
              </div>
            ))}
            <div className="flex gap-2 mt-4">
              <button
                onClick={saveProfile}
                disabled={profileSaving}
                className="px-4 py-2 bg-primary text-fg-on-primary rounded-xl text-sm hover:bg-primary-hover disabled:opacity-50"
              >
                {profileSaving ? 'Saving...' : 'Save'}
              </button>
              <button
                onClick={() => {
                  setEditingProfile(false)
                  setEditMix(null)
                }}
                className="px-4 py-2 rounded-xl text-sm text-fg-secondary bg-muted"
              >
                Cancel
              </button>
            </div>
          </div>
        ) : profile ? (
          <div className="space-y-3">
            {TASK_KEYS.map(key => (
              <div key={key} className="flex items-center gap-3">
                <span className="w-28 text-sm text-fg-secondary">{TASK_LABELS[key]}</span>
                <div className="flex-1 h-2 rounded-full bg-muted overflow-hidden">
                  <div
                    className="h-full rounded-full bg-primary"
                    style={{ width: `${Math.round(profile.task_mix[key] * 100)}%` }}
                  />
                </div>
                <span className="w-10 text-right text-sm text-fg-muted">
                  {Math.round(profile.task_mix[key] * 100)}%
                </span>
              </div>
            ))}
            <button
              onClick={() => {
                setEditingProfile(true)
                setEditMix({ ...profile.task_mix })
              }}
              className="text-sm text-primary hover:text-primary-hover mt-2"
            >
              Edit profile
            </button>
          </div>
        ) : (
          <button
            onClick={() => {
              setEditingProfile(true)
              setEditMix({
                writing: 0.15,
                coding: 0.15,
                research: 0.2,
                planning: 0.15,
                communication: 0.2,
                other: 0.15,
              })
            }}
            className="w-full py-3 border-2 border-dashed border-default rounded-2xl text-fg-muted hover:text-fg-active"
          >
            + Set up your AI usage profile
          </button>
        )}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-fg mb-2">Account</h2>
        <p className="text-sm text-fg-muted mb-2">Signed in as {user?.email}</p>
        {showDeleteConfirm ? (
          <div className="p-4 rounded-2xl space-y-3 bg-error border border-default">
            <p className="text-fg-error text-sm font-medium">
              This will permanently delete your account and all data. This cannot be undone.
            </p>
            <div className="flex gap-2">
              <button onClick={handleDelete} className="px-4 py-2 rounded-xl text-sm hover:opacity-90 bg-destructive text-fg-on-primary">
                Yes, delete everything
              </button>
              <button onClick={() => setShowDeleteConfirm(false)} className="px-4 py-2 rounded-xl text-sm text-fg-secondary bg-muted">
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
