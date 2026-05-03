import { useEffect, useState } from 'react'
import { getWeeklyTrends, getMonthlyTrends, getTimeSaved } from '../api/trends'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'
import { TrendChart } from '../components/TrendChart'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { getDomain } from '../data/domains'

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

  if (loading) return <div className="text-center py-16 text-gray-500">Loading trends...</div>

  const domainData = timeSaved ? Object.entries(timeSaved.byDomain).map(([domain, saved]) => ({
    name: getDomain(domain).name,
    value: saved,
    color: getDomain(domain).color,
  })) : []

  const activityData = timeSaved ? Object.entries(timeSaved.byActivity).map(([activity, saved]) => ({
    name: activity,
    value: saved,
  })) : []

  return (
    <div className="space-y-8">
      <div className="flex gap-2">
        <button onClick={() => setView('weekly')} className={`px-3 py-1 rounded text-sm ${view === 'weekly' ? 'bg-indigo-600 text-white' : 'bg-gray-200'}`}>Weekly</button>
        <button onClick={() => setView('monthly')} className={`px-3 py-1 rounded text-sm ${view === 'monthly' ? 'bg-indigo-600 text-white' : 'bg-gray-200'}`}>Monthly</button>
      </div>

      <TrendChart data={view === 'weekly' ? weekly : monthly.map(m => ({ week: m.month, domain: m.domain, totalSeconds: m.totalSeconds, totalSaved: m.totalSaved }))} />

      {domainData.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-sm text-gray-500 uppercase tracking-wide mb-4">Time saved by tool</h3>
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
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-sm text-gray-500 uppercase tracking-wide mb-4">What you use AI for</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={activityData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}>
                {activityData.map((_, i) => <Cell key={i} fill={['#6366F1', '#10B981', '#F59E0B', '#EF4444'][i % 4]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
