import type { TimeSaved } from '../types'

export function TimeSavedCard({ data }: { data: TimeSaved }) {
  const hours = Math.floor(data.total / 60)
  const minutes = data.total % 60
  const display = hours > 0 ? `${hours},${Math.round((minutes / 60) * 10)}h` : `${minutes}m`

  return (
    <div className="flex flex-col items-center py-8">
      <p className="text-6xl font-bold font-serif" style={{ color: '#8fb87a' }}>{display}</p>
      <p className="text-sm uppercase tracking-widest mt-1" style={{ color: '#5C7A52', letterSpacing: '2px' }}>
        Time Saved
      </p>
    </div>
  )
}
