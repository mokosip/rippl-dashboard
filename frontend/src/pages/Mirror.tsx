import { useEffect, useState } from 'react'
import { getMirrorMoments } from '../api/insights'
import type { MirrorMoment } from '../types'
import { MirrorMomentCard } from '../components/MirrorMomentCard'

export function Mirror() {
  const [moments, setMoments] = useState<MirrorMoment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMirrorMoments()
      .then(setMoments)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16 text-fg-muted">Loading insights...</div>

  if (moments.length === 0) {
    return (
      <div className="text-center py-16">
        <p className="text-fg-secondary">Not enough data yet for mirror moments.</p>
        <p className="text-sm mt-2 text-fg-muted">Keep using AI tools and check back soon.</p>
      </div>
    )
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-fg mb-6">Mirror Moments</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {moments.map((m, i) => <MirrorMomentCard key={i} moment={m} index={i} />)}
      </div>
    </div>
  )
}
