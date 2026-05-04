import type { CollectorInfo } from '../types'

interface Props {
  collector: CollectorInfo
  onRemove: (id: string) => void
}

const TYPE_LABELS: Record<string, string> = {
  chrome_extension: 'Chrome Extension',
}

export function CollectorCard({ collector, onRemove }: Props) {
  return (
    <div className="pond-card flex items-center justify-between">
      <div>
        <p className="font-medium" style={{ color: '#c8dfc0' }}>{TYPE_LABELS[collector.type] ?? collector.type}</p>
        {collector.lastSyncAt ? (
          <p className="text-sm" style={{ color: '#8fb87a' }}>
            Connected — last sync at {new Date(collector.lastSyncAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </p>
        ) : (
          <p className="text-sm" style={{ color: '#5C7A52' }}>
            Pending — waiting for first sync
          </p>
        )}
      </div>
      <button
        onClick={() => onRemove(collector.id)}
        className="text-sm"
        style={{ color: '#f87171' }}
      >
        Remove
      </button>
    </div>
  )
}
