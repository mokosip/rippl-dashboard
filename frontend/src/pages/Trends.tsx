import { useEffect, useState } from 'react'
import { getWeeklyTrends, getMonthlyTrends, getTimeSaved } from '../api/trends'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'
import { TrendChart } from '../components/TrendChart'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { getDomain } from '../data/domains'

const ACTIVITY_COLORS = ['#5C7A52', '#B05F3F', '#8B4830', '#1F4E68']

export function Trends() {
  const [weekly, setWeekly] = useState<WeeklyTrend[]>([])
  const [timeSaved, setTimeSaved] = useState<TimeSaved | null>(null)
  const [view, setView] = useState<'weekly' | 'monthly'>('weekly')
  const [monthly, setMonthly] = useState<MonthlyTrend[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getWeeklyTrends(), getMonthlyTrends(), getTimeSaved()])
      .then(([w, m, ts]) => { setWeekly(w); setMonthly(m); setTimeSaved(ts) })
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
    const entries = Object.entries(timeSaved.byActivity).map(([activity, saved]) => ({
      name: activity,
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

  return (
    <div className="space-y-8">
      <div className="flex gap-2">
        <button onClick={() => setView('weekly')} className={`px-3 py-1 rounded-input text-sm ${view === 'weekly' ? 'bg-primary text-fg-on-primary' : 'bg-muted text-fg-secondary'}`}>Weekly</button>
        <button onClick={() => setView('monthly')} className={`px-3 py-1 rounded-input text-sm ${view === 'monthly' ? 'bg-primary text-fg-on-primary' : 'bg-muted text-fg-secondary'}`}>Monthly</button>
      </div>

      <TrendChart data={view === 'weekly' ? weekly : monthly.map(m => ({ week: m.month, domain: m.domain, totalSeconds: m.totalSeconds, totalSaved: m.totalSaved }))} />

      {domainData.length > 0 && (
        <div className="bg-card rounded-card shadow-sm p-6">
          <h3 className="text-sm text-fg-muted uppercase tracking-wide mb-4">Time saved by tool</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={domainData} layout="vertical">
              <XAxis type="number" unit=" min" />
              <YAxis type="category" dataKey="name" width={100} />
              <Tooltip />
              <Bar dataKey="value" name="Minutes saved">
                {domainData.map((d, i) => <Cell key={i} fill={d.color} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {activityData.length > 0 && (
        <div className="bg-card rounded-card shadow-sm p-6">
          <h3 className="text-sm text-fg-muted uppercase tracking-wide mb-4">What you use AI for</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={activityData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}>
                {activityData.map((_, i) => <Cell key={i} fill={ACTIVITY_COLORS[i % ACTIVITY_COLORS.length]} />)}
              </Pie>
              <Tooltip content={({ active, payload }) => {
                if (!active || !payload?.[0]) return null
                const data = payload[0].payload
                return (
                  <div className="bg-card rounded-card shadow-sm p-2 text-sm border border-border">
                    <p className="font-medium">{data.name}: {data.value} min</p>
                    {data.breakdown?.map((b: { name: string; value: number }) => (
                      <p key={b.name} className="text-fg-muted">{b.name}: {b.value} min</p>
                    ))}
                  </div>
                )
              }} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
