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
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Data Sources</h2>
        {loading ? (
          <p className="text-gray-500">Loading...</p>
        ) : (
          <div className="space-y-3">
            {collectors.map(c => (
              <CollectorCard key={c.id} collector={c} onRemove={handleRemove} />
            ))}
            {!collectors.some(c => c.type === 'chrome_extension') && (
              <button
                onClick={handleAddExtension}
                className="w-full py-3 border-2 border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-indigo-400 hover:text-indigo-600"
              >
                + Connect Chrome Extension
              </button>
            )}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-2">Account</h2>
        <p className="text-sm text-gray-500 mb-2">Signed in as {user?.email}</p>
        {showDeleteConfirm ? (
          <div className="bg-red-50 p-4 rounded space-y-3">
            <p className="text-red-700 text-sm font-medium">
              This will permanently delete your account and all data. This cannot be undone.
            </p>
            <div className="flex gap-2">
              <button onClick={handleDelete} className="px-4 py-2 bg-red-600 text-white rounded text-sm hover:bg-red-700">
                Yes, delete everything
              </button>
              <button onClick={() => setShowDeleteConfirm(false)} className="px-4 py-2 bg-gray-200 rounded text-sm">
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <button
            onClick={() => setShowDeleteConfirm(true)}
            className="text-red-600 hover:text-red-800 text-sm"
          >
            Delete my account
          </button>
        )}
      </section>
    </div>
  )
}
