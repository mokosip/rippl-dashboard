import { useEffect, useState } from 'react'
import { getWeeklyTrends, getMonthlyTrends, getTimeSaved, getActivityHeatmap } from '../api/trends'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'
import { PondChart } from '../components/PondChart'
import { ActivityPond } from '../components/ActivityPond'
import { WaveBarChart } from '../components/WaveBarChart'
import { RippleSpider } from '../components/RippleSpider'
import { getDomain } from '../data/domains'

export function Trends() {
  const [weekly, setWeekly] = useState<WeeklyTrend[]>([])
  const [timeSaved, setTimeSaved] = useState<TimeSaved | null>(null)
  const [view, setView] = useState<'weekly' | 'monthly'>('weekly')
  const [monthly, setMonthly] = useState<MonthlyTrend[]>([])
  const [heatmapData, setHeatmapData] = useState<number[][] | undefined>()
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getWeeklyTrends(), getMonthlyTrends(), getTimeSaved(), getActivityHeatmap()])
      .then(([w, m, ts, hm]) => { setWeekly(w); setMonthly(m); setTimeSaved(ts); setHeatmapData(hm) })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16 text-fg-muted">Loading trends...</div>

  const domainData = timeSaved ? Object.entries(timeSaved.byDomain).map(([domain, saved]) => ({
    name: getDomain(domain).name,
    value: saved,
    color: getDomain(domain).color,
  })) : []

  const activityData = (() => {
    if (!timeSaved) return []
    const entries = Object.entries(timeSaved.byTaskMix ?? {}).map(([task, saved]) => ({
      name: task,
      value: saved,
      breakdown: undefined as { name: string; value: number }[] | undefined,
    }))
    const total = entries.reduce((sum, e) => sum + e.value, 0)
    if (total === 0) return entries
    const significant = entries.filter(e => (e.value / total) >= 0.01)
    const others = entries.filter(e => (e.value / total) < 0.01)
    const othersValue = others.reduce((sum, e) => sum + e.value, 0)
    if (othersValue > 0) significant.push({ name: 'Others', value: othersValue, breakdown: others })
    return significant
  })()

  const trendData = view === 'weekly' ? weekly : monthly.map(m => ({
    week: m.month, domain: m.domain, totalSeconds: m.totalSeconds, totalSaved: m.totalSaved, confidence: m.confidence
  }))

  return (
    <div className="space-y-8">
      <div className="flex gap-2">
        {(['weekly', 'monthly'] as const).map(v => (
          <button key={v} onClick={() => setView(v)}
            className={`px-4 py-1.5 rounded-full text-sm capitalize border ${
              view === v
                ? 'bg-accent text-fg border-default'
                : 'text-fg-secondary border-subtle'
            }`}>
            {v}
          </button>
        ))}
      </div>

      <PondChart data={trendData} />

      <ActivityPond data={heatmapData} />

      <WaveBarChart data={domainData} />

      <RippleSpider data={activityData} />
    </div>
  )
}
