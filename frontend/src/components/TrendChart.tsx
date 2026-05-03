import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { WeeklyTrend } from '../types'
import { getDomain } from '../data/domains'

interface ChartData {
  week: string
  [domain: string]: number | string
}

export function TrendChart({ data }: { data: WeeklyTrend[] }) {
  const domains = [...new Set(data.map(d => d.domain))]

  const byWeek: Record<string, ChartData> = {}
  for (const d of data) {
    if (!byWeek[d.week]) byWeek[d.week] = { week: d.week }
    byWeek[d.week][d.domain] = Math.round(d.totalSeconds / 60)
  }
  const chartData = Object.values(byWeek).sort((a, b) => a.week.localeCompare(b.week))

  if (chartData.length === 0) return null

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-sm text-gray-500 uppercase tracking-wide mb-4">AI Usage (minutes/week)</h3>
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={chartData}>
          <XAxis dataKey="week" tickFormatter={w => new Date(w).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} />
          <YAxis />
          <Tooltip />
          {domains.map(domain => (
            <Area
              key={domain}
              type="monotone"
              dataKey={domain}
              stackId="1"
              fill={getDomain(domain).color}
              stroke={getDomain(domain).color}
              name={getDomain(domain).name}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
