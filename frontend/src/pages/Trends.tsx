import { useEffect, useState } from 'react'
import { getWeeklyTrends, getMonthlyTrends, getTimeSaved } from '../api/trends'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'
import { PondChart } from '../components/PondChart'
import { ActivityPond } from '../components/ActivityPond'
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

  const trendData = view === 'weekly' ? weekly : monthly.map(m => ({
    week: m.month, domain: m.domain, totalSeconds: m.totalSeconds, totalSaved: m.totalSaved
  }))

  return (
    <div className="space-y-8">
      <div className="flex gap-2">
        {(['weekly', 'monthly'] as const).map(v => (
          <button key={v} onClick={() => setView(v)}
            className="px-4 py-1.5 rounded-full text-sm capitalize"
            style={{
              background: view === v ? 'rgba(92,122,82,0.15)' : 'transparent',
              color: view === v ? '#c8dfc0' : '#6a9a5a',
              border: `1px solid ${view === v ? 'rgba(92,122,82,0.4)' : 'rgba(92,122,82,0.2)'}`,
            }}>
            {v}
          </button>
        ))}
      </div>

      <PondChart data={trendData} />

      <ActivityPond />

      {domainData.length > 0 && (
        <div className="pond-card">
          <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1px' }}>
            Time saved by tool
          </p>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={domainData} layout="vertical">
              <XAxis type="number" unit=" min" tick={{ fill: '#5C7A52', fontSize: 11 }} axisLine={{ stroke: 'rgba(92,122,82,0.15)' }} />
              <YAxis type="category" dataKey="name" width={100} tick={{ fill: '#6a9a5a', fontSize: 12 }} axisLine={false} />
              <Tooltip contentStyle={{ background: 'rgba(15,26,15,0.9)', border: '1px solid rgba(92,122,82,0.3)', borderRadius: 12, color: '#c8dfc0' }} />
              <Bar dataKey="value" name="Minutes saved" radius={[0, 4, 4, 0]}>
                {domainData.map((d, i) => <Cell key={i} fill={d.color} opacity={0.8} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {activityData.length > 0 && (
        <div className="pond-card">
          <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1px' }}>
            What you use AI for
          </p>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={activityData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90}
                label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}>
                {activityData.map((_, i) => <Cell key={i} fill={ACTIVITY_COLORS[i % ACTIVITY_COLORS.length]} opacity={0.8} />)}
              </Pie>
              <Tooltip content={({ active, payload }) => {
                if (!active || !payload?.[0]) return null
                const data = payload[0].payload
                return (
                  <div style={{ background: 'rgba(15,26,15,0.9)', border: '1px solid rgba(92,122,82,0.3)', borderRadius: 12, padding: '8px 12px', color: '#c8dfc0', fontSize: 13 }}>
                    <p className="font-medium">{data.name}: {data.value} min</p>
                    {data.breakdown?.map((b: { name: string; value: number }) => (
                      <p key={b.name} style={{ color: '#6a9a5a' }}>{b.name}: {b.value} min</p>
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
