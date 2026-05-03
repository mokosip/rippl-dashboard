import { useEffect, useState } from 'react'
import { getWeeklyTrends, getTimeSaved } from '../api/trends'
import { getMirrorMoments } from '../api/insights'
import type { WeeklyTrend, TimeSaved, MirrorMoment } from '../types'
import { TimeSavedCard } from '../components/TimeSavedCard'
import { TrendChart } from '../components/TrendChart'
import { MirrorMomentCard } from '../components/MirrorMomentCard'
import { OnboardingChecklist } from '../components/OnboardingChecklist'

export function Dashboard() {
  const [trends, setTrends] = useState<WeeklyTrend[]>([])
  const [timeSaved, setTimeSaved] = useState<TimeSaved | null>(null)
  const [insights, setInsights] = useState<MirrorMoment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      getWeeklyTrends(),
      getTimeSaved(),
      getMirrorMoments(),
    ]).then(([t, ts, i]) => {
      setTrends(t)
      setTimeSaved(ts)
      setInsights(i)
    }).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16 text-fg-muted">Loading dashboard...</div>
  if (!timeSaved || timeSaved.total === 0) return <OnboardingChecklist />

  return (
    <div className="space-y-8">
      <TimeSavedCard data={timeSaved} />
      <TrendChart data={trends} />
      {insights.length > 0 && (
        <div>
          <h3 className="text-sm text-fg-muted uppercase tracking-wide mb-4">Mirror Moments</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {insights.map((m, i) => <MirrorMomentCard key={i} moment={m} />)}
          </div>
        </div>
      )}
    </div>
  )
}
