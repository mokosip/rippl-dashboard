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
    <div className="flex items-center justify-between bg-white rounded-lg shadow p-4">
      <div>
        <p className="font-medium text-gray-900">{TYPE_LABELS[collector.type] ?? collector.type}</p>
        <p className="text-sm text-gray-500">
          Connected {new Date(collector.linkedAt).toLocaleDateString()}
        </p>
      </div>
      <button
        onClick={() => onRemove(collector.id)}
        className="text-red-600 hover:text-red-800 text-sm"
      >
        Remove
      </button>
    </div>
  )
}
