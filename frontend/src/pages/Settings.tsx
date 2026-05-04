import { useEffect, useState } from 'react'
import { getCollectors, addCollector, removeCollector } from '../api/collectors'
import { deleteAccount } from '../api/account'
import type { CollectorInfo } from '../types'
import { CollectorCard } from '../components/CollectorCard'
import { useAuth } from '../hooks/useAuth'
import { useExtension } from '../hooks/useExtension'

export function Settings() {
  const { user } = useAuth()
  const { status: extStatus, sendToken } = useExtension()
  const [collectors, setCollectors] = useState<CollectorInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [pendingToken, setPendingToken] = useState<string | null>(null)
  const [connecting, setConnecting] = useState(false)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    getCollectors().then(setCollectors).finally(() => setLoading(false))
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
              <div className="bg-accent p-4 rounded-card space-y-2">
                <p className="text-sm font-medium text-fg">Extension API Token</p>
                <p className="text-xs text-fg-muted">Copy this token — it won't be shown again.</p>
                <div className="flex gap-2">
                  <code className="flex-1 px-3 py-2 bg-card rounded-input text-xs text-fg-secondary break-all border border-default">
                    {pendingToken}
                  </code>
                  <button
                    onClick={handleCopy}
                    className="px-3 py-2 bg-primary text-fg-on-primary rounded-input text-xs hover:bg-primary-hover"
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
                    className="w-full py-3 bg-primary text-fg-on-primary rounded-card hover:bg-primary-hover disabled:opacity-50"
                  >
                    {connecting ? 'Connecting...' : 'Connect Chrome Extension'}
                  </button>
                ) : extStatus === 'not_installed' ? (
                  <div className="space-y-2">
                    <button
                      onClick={handleConnect}
                      disabled={connecting}
                      className="w-full py-3 border-2 border-dashed border-default rounded-card text-fg-muted hover:border-ring hover:text-fg-active"
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
