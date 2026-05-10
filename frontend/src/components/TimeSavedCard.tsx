import type { TimeSaved } from '../types'

const CONFIDENCE_DISPLAY = {
  high: { prefix: '', label: 'high confidence' },
  medium: { prefix: '~', label: 'estimate' },
  low: { prefix: '~', label: 'rough estimate' },
} as const

export function TimeSavedCard({ data }: { data: TimeSaved }) {
  const hours = Math.floor(data.total / 60)
  const minutes = data.total % 60
  const display = hours > 0 ? `${hours},${Math.round((minutes / 60) * 10)}h` : `${minutes}m`
  const conf = CONFIDENCE_DISPLAY[data.confidence ?? 'low']

  return (
    <div className="flex flex-col items-center py-8">
      <p className="text-6xl font-bold font-serif text-fg-accent">
        {conf.prefix}{display}
      </p>
      <p className="text-sm uppercase tracking-widest mt-1 text-fg-muted" style={{ letterSpacing: '2px' }}>
        Time Saved
      </p>
      <p className="text-xs text-fg-muted mt-1">{conf.label}</p>
    </div>
  )
}
