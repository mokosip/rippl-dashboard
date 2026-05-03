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
    <div className="flex items-center justify-between bg-card rounded-card shadow-sm p-4">
      <div>
        <p className="font-medium text-fg">{TYPE_LABELS[collector.type] ?? collector.type}</p>
        <p className="text-sm text-fg-muted">
          Connected {new Date(collector.linkedAt).toLocaleDateString()}
        </p>
      </div>
      <button
        onClick={() => onRemove(collector.id)}
        className="text-fg-error hover:text-destructive text-sm"
      >
        Remove
      </button>
    </div>
  )
}
