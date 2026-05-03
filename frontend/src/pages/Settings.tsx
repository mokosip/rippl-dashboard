import { useEffect, useState } from 'react'
import { getCollectors, addCollector, removeCollector } from '../api/collectors'
import { deleteAccount } from '../api/account'
import type { CollectorInfo } from '../types'
import { CollectorCard } from '../components/CollectorCard'
import { useAuth } from '../hooks/useAuth'

export function Settings() {
  const { user } = useAuth()
  const [collectors, setCollectors] = useState<CollectorInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  useEffect(() => {
    getCollectors().then(setCollectors).finally(() => setLoading(false))
  }, [])

  const handleAddExtension = async () => {
    const c = await addCollector('chrome_extension')
    setCollectors(prev => [...prev, c])
  }

  const handleRemove = async (id: string) => {
    await removeCollector(id)
    setCollectors(prev => prev.filter(c => c.id !== id))
  }

  const handleDelete = async () => {
    await deleteAccount()
    window.location.href = '/login'
  }

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
            {!collectors.some(c => c.type === 'chrome_extension') && (
              <button
                onClick={handleAddExtension}
                className="w-full py-3 border-2 border-dashed border-default rounded-card text-fg-muted hover:border-ring hover:text-fg-active"
              >
                + Connect Chrome Extension
              </button>
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
