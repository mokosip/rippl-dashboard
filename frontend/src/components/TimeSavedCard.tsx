import type { TimeSaved } from '../types'
import { getComparison } from '../data/comparisons'

export function TimeSavedCard({ data }: { data: TimeSaved }) {
  const hours = Math.floor(data.total / 60)
  const minutes = data.total % 60

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <p className="text-sm text-gray-500 uppercase tracking-wide">Total time saved</p>
      <p className="text-4xl font-bold text-gray-900 mt-1">
        {hours > 0 && <>{hours}h </>}{minutes}m
      </p>
      <p className="text-gray-500 mt-2">That's {getComparison(data.total)}</p>
    </div>
  )
}
