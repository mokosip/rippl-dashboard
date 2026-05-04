import { useEffect, useState } from 'react'
import { getCollectors } from '../api/collectors'

export function SyncIndicator() {
  const [lastSync, setLastSync] = useState<string | null>(null)

  useEffect(() => {
    getCollectors().then(collectors => {
      const synced = collectors.find(c => c.lastSyncAt)
      if (synced?.lastSyncAt) setLastSync(synced.lastSyncAt)
    }).catch(() => {})
  }, [])

  if (!lastSync) return null

  const time = new Date(lastSync)
  const diff = Math.round((Date.now() - time.getTime()) / 60000)
  const label = diff < 1 ? 'just now' : diff < 60 ? `${diff} min ago` : `${Math.round(diff / 60)}h ago`

  return (
    <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-input border border-default text-fg-secondary text-xs">
      <div className="sync-dot" />
      <span>synced {label}</span>
    </div>
  )
}
